package com.example.minecraftmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A rare, world-persistent Overworld thunderstorm variant.
 *
 * <p>The chance is deliberately stored rather than calculated from total game
 * time. That lets an End completion raise the cap without retroactively
 * jumping the chance to the new cap.</p>
 */
public final class LightningWaveRules {
	private static final double NORMAL_START_CHANCE = 0.0000001D; // 0.00001%
	private static final double NORMAL_CHANCE_STEP = 0.0000001D; // +0.00001 percentage points / second
	private static final long NORMAL_GROWTH_INTERVAL_TICKS = 20L;
	private static final double NORMAL_CAP = 0.10D; // 10%
	private static final double NORMAL_COMPLETED_CAP = 0.25D; // 25%

	private static final double HARDCORE_START_CHANCE = 0.0000005D; // 0.00005%
	private static final double HARDCORE_CHANCE_STEP = 0.0000002D; // +0.00002 percentage points / 0.5 second
	private static final long HARDCORE_GROWTH_INTERVAL_TICKS = 10L;
	private static final double HARDCORE_CAP = 0.20D; // 20%
	private static final double HARDCORE_COMPLETED_CAP = 0.50D; // 50%

	private static final long WARNING_DURATION_TICKS = 160L;
	private static final long WARNING_FLASH_INTERVAL_TICKS = 20L;
	private static final int WARNING_FLASH_DURATION_TICKS = 8;
	private static final long WAVE_DURATION_TICKS = 600L;
	private static final long STRIKE_INTERVAL_TICKS = 40L;
	private static final double MIN_STRIKE_DISTANCE = 6.0D;
	private static final double MAX_STRIKE_DISTANCE = 14.0D;
	private static final int STRIKE_POSITION_ATTEMPTS = 8;
	private static final double METAL_ARMOR_TARGET_WEIGHT_PER_PIECE = 0.15D;
	private static final double METAL_ARMOR_EXTRA_STRIKE_CHANCE_PER_PIECE = 0.05D;
	private static final EquipmentSlot[] ARMOR_SLOTS = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private LightningWaveRules() {
	}

	public static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}

		LightningWaveSavedData data = data(level);
		long gameTime = level.getGameTime();
		boolean changed = data.initializeIfNeeded(level.getLevelData().isHardcore(), gameTime);
		changed |= data.refreshCompletion(level);
		changed |= data.growChance(level.getLevelData().isHardcore(), gameTime);

		if (!level.isThundering() && !data.forcedWave) {
			changed |= data.clearStorm();
			if (changed) {
				data.setDirty();
			}
			return;
		}

		if (!data.stormActive) {
			data.stormActive = true;
			changed = true;
			if (!level.players().isEmpty() && level.random.nextDouble() < data.chance) {
				data.startWave(gameTime);
			}
		}

		if (data.hasWave()) {
			if (gameTime < data.warningEndsAt) {
				if (gameTime - data.lastWarningFlashAt >= WARNING_FLASH_INTERVAL_TICKS) {
					sendWarningFlash(level);
					data.lastWarningFlashAt = gameTime;
					changed = true;
				}
			} else if (gameTime < data.waveEndsAt) {
				if (gameTime - data.lastStrikeAt >= STRIKE_INTERVAL_TICKS) {
					tryStrikeNearPlayer(level);
					data.lastStrikeAt = gameTime;
					changed = true;
				}
			} else {
				data.clearWave();
				data.forcedWave = false;
				changed = true;
			}
		}

		if (changed) {
			data.setDirty();
		}
	}

	/** Called once a player returns from the End, without changing the current chance. */
	public static void onWorldCompleted(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}

		LightningWaveSavedData data = data(level);
		if (!data.completed) {
			data.completed = true;
			data.setDirty();
		}
	}

	public static boolean forceUltraThunder(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		long gameTime = level.getGameTime();
		LightningWaveSavedData data = data(level);
		data.initializeIfNeeded(level.getLevelData().isHardcore(), gameTime);
		data.stormActive = true;
		data.forcedWave = true;
		data.startWave(gameTime);
		level.setWeatherParameters(0, 800, true, true);
		data.setDirty();
		return true;
	}

	private static void sendWarningFlash(ServerLevel level) {
		LightningWaveFlashPayload payload = new LightningWaveFlashPayload(WARNING_FLASH_DURATION_TICKS);
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator()) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private static void tryStrikeNearPlayer(ServerLevel level) {
		ServerPlayer target = chooseTarget(level);
		if (target == null) {
			return;
		}

		spawnLightningNear(level, target);
		int metalPieces = metalArmorPieces(target);
		if (metalPieces > 0 && level.random.nextDouble() < metalPieces * METAL_ARMOR_EXTRA_STRIKE_CHANCE_PER_PIECE) {
			spawnLightningNear(level, target);
		}
	}

	private static void spawnLightningNear(ServerLevel level, ServerPlayer target) {
		BlockPos strikePosition = findStrikePosition(level, target);
		if (strikePosition == null) {
			return;
		}

		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (lightning != null) {
			lightning.setPos(strikePosition.getX() + 0.5D, strikePosition.getY(), strikePosition.getZ() + 0.5D);
			level.addFreshEntity(lightning);
		}
	}

	private static ServerPlayer chooseTarget(ServerLevel level) {
		List<ServerPlayer> candidates = new ArrayList<>();
		double totalWeight = 0.0D;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || !player.isAlive() || !level.canSeeSky(player.blockPosition())) {
				continue;
			}
			candidates.add(player);
			totalWeight += 1.0D + metalArmorPieces(player) * METAL_ARMOR_TARGET_WEIGHT_PER_PIECE;
		}

		if (candidates.isEmpty()) {
			return null;
		}

		double selectedWeight = level.random.nextDouble() * totalWeight;
		for (ServerPlayer candidate : candidates) {
			selectedWeight -= 1.0D + metalArmorPieces(candidate) * METAL_ARMOR_TARGET_WEIGHT_PER_PIECE;
			if (selectedWeight <= 0.0D) {
				return candidate;
			}
		}
		return candidates.getLast();
	}

	private static BlockPos findStrikePosition(ServerLevel level, ServerPlayer target) {
		for (int attempt = 0; attempt < STRIKE_POSITION_ATTEMPTS; attempt++) {
			double angle = level.random.nextDouble() * Math.PI * 2.0D;
			double radius = MIN_STRIKE_DISTANCE + level.random.nextDouble() * (MAX_STRIKE_DISTANCE - MIN_STRIKE_DISTANCE);
			int x = (int)Math.floor(target.getX() + Math.cos(angle) * radius);
			int z = (int)Math.floor(target.getZ() + Math.sin(angle) * radius);
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}

			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos position = new BlockPos(x, y, z);
			if (level.canSeeSky(position)) {
				return position;
			}
		}
		return null;
	}

	private static int metalArmorPieces(ServerPlayer player) {
		int count = 0;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (isMetalArmor(player.getItemBySlot(slot))) {
				count++;
			}
		}
		return count;
	}

	private static boolean isMetalArmor(ItemStack stack) {
		Item item = stack.getItem();
		return item == Items.CHAINMAIL_HELMET
			|| item == Items.CHAINMAIL_CHESTPLATE
			|| item == Items.CHAINMAIL_LEGGINGS
			|| item == Items.CHAINMAIL_BOOTS
			|| item == Items.IRON_HELMET
			|| item == Items.IRON_CHESTPLATE
			|| item == Items.IRON_LEGGINGS
			|| item == Items.IRON_BOOTS
			|| item == Items.GOLDEN_HELMET
			|| item == Items.GOLDEN_CHESTPLATE
			|| item == Items.GOLDEN_LEGGINGS
			|| item == Items.GOLDEN_BOOTS
			|| item == Items.NETHERITE_HELMET
			|| item == Items.NETHERITE_CHESTPLATE
			|| item == Items.NETHERITE_LEGGINGS
			|| item == Items.NETHERITE_BOOTS;
	}

	private static LightningWaveSavedData data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(LightningWaveSavedData.TYPE);
	}

	private static final class LightningWaveSavedData extends SavedData {
		private static final Codec<LightningWaveSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.DOUBLE.optionalFieldOf("chance", 0.0D).forGetter(data -> data.chance),
			Codec.LONG.optionalFieldOf("last_growth_tick", 0L).forGetter(data -> data.lastGrowthTick),
			Codec.BOOL.optionalFieldOf("initialized", false).forGetter(data -> data.initialized),
			Codec.BOOL.optionalFieldOf("completed", false).forGetter(data -> data.completed),
			Codec.BOOL.optionalFieldOf("storm_active", false).forGetter(data -> data.stormActive),
			Codec.BOOL.optionalFieldOf("forced_wave", false).forGetter(data -> data.forcedWave),
			Codec.LONG.optionalFieldOf("warning_ends_at", -1L).forGetter(data -> data.warningEndsAt),
			Codec.LONG.optionalFieldOf("wave_ends_at", -1L).forGetter(data -> data.waveEndsAt),
			Codec.LONG.optionalFieldOf("last_warning_flash_at", Long.MIN_VALUE).forGetter(data -> data.lastWarningFlashAt),
			Codec.LONG.optionalFieldOf("last_strike_at", Long.MIN_VALUE).forGetter(data -> data.lastStrikeAt)
		).apply(instance, LightningWaveSavedData::new));
		private static final SavedDataType<LightningWaveSavedData> TYPE = new SavedDataType<>(
			"minecraftmod_lightning_wave",
			LightningWaveSavedData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
		);

		private double chance;
		private long lastGrowthTick;
		private boolean initialized;
		private boolean completed;
		private boolean stormActive;
		private boolean forcedWave;
		private long warningEndsAt;
		private long waveEndsAt;
		private long lastWarningFlashAt;
		private long lastStrikeAt;

		private LightningWaveSavedData() {
			this(0.0D, 0L, false, false, false, false, -1L, -1L, Long.MIN_VALUE, Long.MIN_VALUE);
		}

		private LightningWaveSavedData(
			double chance,
			long lastGrowthTick,
			boolean initialized,
			boolean completed,
			boolean stormActive,
			boolean forcedWave,
			long warningEndsAt,
			long waveEndsAt,
			long lastWarningFlashAt,
			long lastStrikeAt
		) {
			this.chance = chance;
			this.lastGrowthTick = lastGrowthTick;
			this.initialized = initialized;
			this.completed = completed;
			this.stormActive = stormActive;
			this.forcedWave = forcedWave;
			this.warningEndsAt = warningEndsAt;
			this.waveEndsAt = waveEndsAt;
			this.lastWarningFlashAt = lastWarningFlashAt;
			this.lastStrikeAt = lastStrikeAt;
		}

		private boolean initializeIfNeeded(boolean hardcore, long gameTime) {
			if (initialized) {
				return false;
			}
			chance = hardcore ? HARDCORE_START_CHANCE : NORMAL_START_CHANCE;
			lastGrowthTick = gameTime;
			initialized = true;
			return true;
		}

		private boolean refreshCompletion(ServerLevel level) {
			if (completed) {
				return false;
			}
			for (ServerPlayer player : level.players()) {
				if (EndCompletionRules.isCompleted(player)) {
					completed = true;
					return true;
				}
			}
			return false;
		}

		private boolean growChance(boolean hardcore, long gameTime) {
			long interval = hardcore ? HARDCORE_GROWTH_INTERVAL_TICKS : NORMAL_GROWTH_INTERVAL_TICKS;
			if (gameTime < lastGrowthTick) {
				lastGrowthTick = gameTime;
				return true;
			}

			long steps = (gameTime - lastGrowthTick) / interval;
			if (steps <= 0L) {
				return false;
			}

			lastGrowthTick += steps * interval;
			double cap = hardcore
				? completed ? HARDCORE_COMPLETED_CAP : HARDCORE_CAP
				: completed ? NORMAL_COMPLETED_CAP : NORMAL_CAP;
			chance = Math.min(cap, chance + steps * (hardcore ? HARDCORE_CHANCE_STEP : NORMAL_CHANCE_STEP));
			// Persist the advanced clock even if the chance is currently capped.
			// Otherwise a later End completion could grant retroactive growth after a restart.
			return true;
		}

		private void startWave(long gameTime) {
			warningEndsAt = gameTime + WARNING_DURATION_TICKS;
			waveEndsAt = warningEndsAt + WAVE_DURATION_TICKS;
			lastWarningFlashAt = gameTime - WARNING_FLASH_INTERVAL_TICKS;
			lastStrikeAt = warningEndsAt - STRIKE_INTERVAL_TICKS;
		}

		private boolean hasWave() {
			return waveEndsAt >= 0L;
		}

		private boolean clearStorm() {
			boolean changed = stormActive || forcedWave || hasWave();
			stormActive = false;
			forcedWave = false;
			clearWave();
			return changed;
		}

		private void clearWave() {
			warningEndsAt = -1L;
			waveEndsAt = -1L;
			lastWarningFlashAt = Long.MIN_VALUE;
			lastStrikeAt = Long.MIN_VALUE;
		}
	}
}
