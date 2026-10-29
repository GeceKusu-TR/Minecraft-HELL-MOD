package com.example.minecraftmod;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.example.minecraftmod.mixin.PlayerInfoUpdatePacketAccessor;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class GrassCurseRules {
	private static final String CURSED_KEY = "minecraftmod_cursed_grass";
	private static final String REMAINING_TICKS_KEY = "remaining_ticks";
	private static final String AFTERMATH_TAG_PREFIX = "minecraftmod_grass_aftermath_";
	private static final String FAKE_PLAYER_NAME = "kk88ff";
	private static final UUID FAKE_PLAYER_UUID = UUID.nameUUIDFromBytes("minecraftmod:kk88ff".getBytes(java.nio.charset.StandardCharsets.UTF_8));
	private static final String BLACK_SKIN_TEXTURE_URL = "http://textures.minecraft.net/texture/c7acd6b5a7a61bb25892225d160138b4e97fa02cc33fbecb559deae4cb485f";
	private static final int TOTAL_TICKS = 20 * 10;
	private static final int MESSAGE_INTERVAL_TICKS = 20;
	private static final int TELEPORT_START_TICKS = 20 * 2;
	private static final int TELEPORT_INTERVAL_TICKS = 20;
	private static final int TELEPORT_DURATION_TICKS = 20 * 10;
	private static final int TELEPORT_RANGE = 64;
	private static final int TELEPORT_ATTEMPTS = 24;
	private static final int MIN_FAKE_PORT = 1024;
	private static final int MAX_FAKE_PORT = 65535;
	private static final long NIGHT_TIME = 18000L;
	private static final long DAY_TIME = 6000L;
	private static final Map<UUID, AftermathSession> AFTERMATH_SESSIONS = new ConcurrentHashMap<>();

	private GrassCurseRules() {
	}

	public static void tickPlayer(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		tickAftermath(player, level);

		Inventory inventory = player.getInventory();
		int existingCurseSlot = -1;
		for (int slot = 0; slot <= inventory.getContainerSize(); slot++) {
			ItemStack stack = slot == inventory.getContainerSize() ? player.containerMenu.getCarried() : inventory.getItem(slot);
			if (isActiveCursedGrass(stack)) {
				existingCurseSlot = slot;
				break;
			}
		}
		boolean keptOne = false;
		for (int slot = 0; slot <= inventory.getContainerSize(); slot++) {
			ItemStack stack = slot == inventory.getContainerSize() ? player.containerMenu.getCarried() : inventory.getItem(slot);
			if (!stack.is(Items.GRASS_BLOCK)) {
				continue;
			}

			if (keptOne || existingCurseSlot >= 0 && slot != existingCurseSlot) {
				clearSlot(player, slot);
				continue;
			}

			keptOne = true;
			ensureCursed(stack);
			stack.setCount(1);

			int remainingTicks = remainingTicks(stack);
			if (remainingTicks <= 0) {
				clearSlot(player, slot);
				continue;
			}

			remainingTicks--;
			if (remainingTicks <= 0) {
				clearSlot(player, slot);
				startAftermath(player);
			} else {
				setRemainingTicks(stack, remainingTicks);
			}
		}
	}

	public static boolean shouldForceNight(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (hasActiveCursedGrass(player)) {
				return true;
			}
		}
		return false;
	}

	public static long forcedTime(ServerLevel level) {
		return shouldForceNight(level) ? NIGHT_TIME : DAY_TIME;
	}

	public static boolean isCursedGrass(ItemStack stack) {
		if (!stack.is(Items.GRASS_BLOCK)) {
			return false;
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return false;
		}

		return customData.copyTag().getBooleanOr(CURSED_KEY, false);
	}

	public static boolean isActiveCursedGrass(ItemStack stack) {
		return isCursedGrass(stack) && remainingTicks(stack) > 0;
	}

	public static boolean hasActiveCursedGrass(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (isActiveCursedGrass(inventory.getItem(slot))) {
				return true;
			}
		}
		return isActiveCursedGrass(player.containerMenu.getCarried());
	}

	public static void onDisconnect(ServerPlayer player) {
		AftermathSession session = AFTERMATH_SESSIONS.remove(player.getUUID());
		if (session != null) {
			saveAftermathTag(player, session);
		}
	}

	public static void retainDroppedGrass(ServerPlayer player, ItemStack stack) {
		if (hasActiveCursedGrass(player)) {
			return;
		}
		ItemStack copy = stack.copyWithCount(1);
		if (!player.getInventory().add(copy)) {
			ItemStack displaced = player.getInventory().getItem(0);
			player.getInventory().setItem(0, copy);
			if (!isCursedGrass(displaced)) player.drop(displaced, false, false);
		}
	}

	private static void clearSlot(ServerPlayer player, int slot) {
		if (slot == player.getInventory().getContainerSize()) {
			player.containerMenu.setCarried(ItemStack.EMPTY);
		} else {
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
	}

	public static void transferCursedGrassAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
		ItemStack cursedGrass = findActiveCursedGrass(oldPlayer);
		if (cursedGrass.isEmpty() || hasActiveCursedGrass(newPlayer)) {
			return;
		}

		ItemStack copy = cursedGrass.copyWithCount(1);
		if (!newPlayer.getInventory().add(copy)) {
			newPlayer.drop(newPlayer.getInventory().getItem(0), false, false);
			newPlayer.getInventory().setItem(0, copy);
		}
	}

	private static ItemStack findActiveCursedGrass(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (isActiveCursedGrass(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void ensureCursed(ItemStack stack) {
		if (!isCursedGrass(stack)) {
			setRemainingTicks(stack, TOTAL_TICKS);
			return;
		}

		updateVisuals(stack, remainingTicks(stack));
	}

	private static int remainingTicks(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return TOTAL_TICKS;
		}
		return customData.copyTag().getIntOr(REMAINING_TICKS_KEY, TOTAL_TICKS);
	}

	private static void setRemainingTicks(ItemStack stack, int remainingTicks) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		tag.putBoolean(CURSED_KEY, true);
		tag.putInt(REMAINING_TICKS_KEY, Math.max(0, remainingTicks));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		updateVisuals(stack, remainingTicks);
	}

	private static void updateVisuals(ItemStack stack, int remainingTicks) {
		int clampedTicks = Math.max(0, Math.min(TOTAL_TICKS, remainingTicks));
		int remainingSeconds = (clampedTicks + 19) / 20;
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		stack.set(DataComponents.MAX_DAMAGE, TOTAL_TICKS);
		stack.set(DataComponents.DAMAGE, TOTAL_TICKS - clampedTicks);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(Integer.toString(remainingSeconds)));
	}

	private static void startAftermath(ServerPlayer player) {
		AftermathSession session = new AftermathSession(randomPort(player), 0);
		AFTERMATH_SESSIONS.putIfAbsent(player.getUUID(), session);
		saveAftermathTag(player, session);
	}

	private static void tickAftermath(ServerPlayer player, ServerLevel level) {
		AftermathSession session = AFTERMATH_SESSIONS.get(player.getUUID());
		if (session == null) {
			session = loadAftermathTag(player);
			if (session == null) {
				return;
			}
			AFTERMATH_SESSIONS.put(player.getUUID(), session);
		}

		if (session.age == 0) {
			player.sendSystemMessage(localizedLanMessage(player, session.port));
		} else if (session.age == MESSAGE_INTERVAL_TICKS) {
			player.sendSystemMessage(Component.literal(localizedJoinMessage(player)).withStyle(ChatFormatting.YELLOW));
			addFakePlayerToTab(player);
		}

		int teleportAge = session.age - TELEPORT_START_TICKS;
		if (teleportAge >= 0 && teleportAge < TELEPORT_DURATION_TICKS && teleportAge % TELEPORT_INTERVAL_TICKS == 0) {
			teleportRandomly(player, level);
		}

		session.age++;
		saveAftermathTag(player, session);
		if (session.age > TELEPORT_START_TICKS + TELEPORT_DURATION_TICKS) {
			AFTERMATH_SESSIONS.remove(player.getUUID());
			clearAftermathTags(player);
			removeFakePlayerFromTab(player);
		}
	}

	private static void saveAftermathTag(ServerPlayer player, AftermathSession session) {
		clearAftermathTags(player);
		player.addTag(AFTERMATH_TAG_PREFIX + session.age + "_" + session.port);
	}

	private static AftermathSession loadAftermathTag(ServerPlayer player) {
		for (String tag : player.getTags()) {
			if (!tag.startsWith(AFTERMATH_TAG_PREFIX)) {
				continue;
			}

			String[] parts = tag.substring(AFTERMATH_TAG_PREFIX.length()).split("_", 2);
			if (parts.length != 2) {
				continue;
			}

			try {
				int age = Math.max(0, Integer.parseInt(parts[0]));
				int port = Math.max(MIN_FAKE_PORT, Math.min(MAX_FAKE_PORT, Integer.parseInt(parts[1])));
				return new AftermathSession(port, age);
			} catch (NumberFormatException ignored) {
				clearAftermathTags(player);
				return null;
			}
		}
		return null;
	}

	private static void clearAftermathTags(ServerPlayer player) {
		for (String tag : List.copyOf(player.getTags())) {
			if (tag.startsWith(AFTERMATH_TAG_PREFIX)) {
				player.removeTag(tag);
			}
		}
	}

	private static Component localizedLanMessage(ServerPlayer player, int port) {
		Component portComponent = Component.literal("[" + port + "]").withStyle(ChatFormatting.GREEN);
		if (usesTurkish(player)) {
			return Component.literal("Yerel oyun ")
				.withStyle(ChatFormatting.WHITE)
				.append(portComponent)
				.append(Component.literal(" ba\u011flant\u0131 noktas\u0131ndan sunuluyor").withStyle(ChatFormatting.WHITE));
		}

		return Component.literal("Local game hosted on port ")
			.withStyle(ChatFormatting.WHITE)
			.append(portComponent);
	}

	private static String localizedJoinMessage(ServerPlayer player) {
		return usesTurkish(player) ? "kk88ff oyuna kat\u0131ld\u0131" : "kk88ff joined the game";
	}

	private static boolean usesTurkish(ServerPlayer player) {
		String language = player.clientInformation().language();
		return language != null && language.toLowerCase(Locale.ROOT).startsWith("tr");
	}

	private static int randomPort(ServerPlayer player) {
		return MIN_FAKE_PORT + player.getRandom().nextInt(MAX_FAKE_PORT - MIN_FAKE_PORT + 1);
	}

	private static void addFakePlayerToTab(ServerPlayer player) {
		Multimap<String, Property> rawProperties = HashMultimap.create();
		rawProperties.put("textures", new Property("textures", blackSkinTextureValue()));
		GameProfile fakeProfile = new GameProfile(FAKE_PLAYER_UUID, FAKE_PLAYER_NAME, new PropertyMap(rawProperties));
		ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
			FAKE_PLAYER_UUID,
			fakeProfile,
			true,
			47,
			GameType.SURVIVAL,
			Component.literal(FAKE_PLAYER_NAME),
			true,
			0,
			null
		);
		ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
			EnumSet.of(
				ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
			),
			List.of()
		);
		((PlayerInfoUpdatePacketAccessor)packet).minecraftmod$setEntries(List.of(entry));
		player.connection.send(packet);
	}

	private static void removeFakePlayerFromTab(ServerPlayer player) {
		player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(FAKE_PLAYER_UUID)));
	}

	private static String blackSkinTextureValue() {
		String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + BLACK_SKIN_TEXTURE_URL + "\",\"metadata\":{\"model\":\"classic\"}}}}";
		return Base64.getEncoder().encodeToString(textureJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private static void teleportRandomly(ServerPlayer player, ServerLevel level) {
		RandomSource random = player.getRandom();
		BlockPos origin = player.blockPosition();
		for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
			int x = origin.getX() + random.nextInt(TELEPORT_RANGE * 2 + 1) - TELEPORT_RANGE;
			int z = origin.getZ() + random.nextInt(TELEPORT_RANGE * 2 + 1) - TELEPORT_RANGE;
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos feetPos = new BlockPos(x, y, z);

			if (y <= level.getMinY() || y >= level.getMaxY() - 1) {
				continue;
			}

			if (!level.getWorldBorder().isWithinBounds(feetPos) || !isSafeTeleportSpot(level, feetPos)) {
				continue;
			}

			player.teleportTo(x + 0.5D, y, z + 0.5D);
			player.fallDistance = 0.0F;
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
			return;
		}
	}

	private static boolean isSafeTeleportSpot(ServerLevel level, BlockPos feetPos) {
		BlockPos belowPos = feetPos.below();
		BlockState belowState = level.getBlockState(belowPos);
		BlockState feetState = level.getBlockState(feetPos);
		BlockState headState = level.getBlockState(feetPos.above());

		if (!feetState.isAir() || !headState.isAir()) {
			return false;
		}

		if (!level.getFluidState(feetPos).isEmpty()
			|| !level.getFluidState(feetPos.above()).isEmpty()
			|| level.getFluidState(belowPos).is(FluidTags.LAVA)) {
			return false;
		}

		if (!belowState.isFaceSturdy(level, belowPos, Direction.UP)) {
			return false;
		}

		return !belowState.is(Blocks.LAVA)
			&& !belowState.is(Blocks.MAGMA_BLOCK)
			&& !belowState.is(Blocks.CACTUS)
			&& !belowState.is(Blocks.CAMPFIRE)
			&& !belowState.is(Blocks.SOUL_CAMPFIRE)
			&& !belowState.is(Blocks.FIRE)
			&& !belowState.is(Blocks.SOUL_FIRE)
			&& !belowState.is(Blocks.SWEET_BERRY_BUSH);
	}

	private static final class AftermathSession {
		private final int port;
		private int age;

		private AftermathSession(int port, int age) {
			this.port = port;
			this.age = age;
		}
	}
}
