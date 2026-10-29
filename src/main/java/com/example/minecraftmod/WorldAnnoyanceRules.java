package com.example.minecraftmod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.example.minecraftmod.mixin.AbstractFurnaceBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WorldAnnoyanceRules {
	private static final Map<ServerLevel, Map<Long, FurnaceOutputSnapshot>> FURNACE_OUTPUTS = new WeakHashMap<>();
	private static final int MAX_DIAMOND_MOVES = 10;

	private WorldAnnoyanceRules() {
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % 10L != 0L) {
			return;
		}

		moveNearbyDiamondOres(level);
		if (level.getGameTime() % 40L == 0L) {
			tickEyeGps(level);
		}
	}

	public static void onCraftTaken(Player player, net.minecraft.world.level.Level menuLevel, BlockPos tablePos) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(menuLevel instanceof ServerLevel level)) {
			return;
		}

		if (!level.getBlockState(tablePos).is(Blocks.CRAFTING_TABLE)) {
			return;
		}

		long key = tablePos.asLong();
		AnnoyanceSavedData data = data(level);
		int uses = data.craftingTableUses.merge(key, 1, Integer::sum);
		int limit = data.craftingTableLimits
			.computeIfAbsent(key, ignored -> 10 + level.random.nextInt(11));
		data.setDirty();
		if (uses < limit) {
			return;
		}

		level.destroyBlock(tablePos, false);
		Block.popResource(level, tablePos, new ItemStack(Items.STICK, 2));
		data.craftingTableUses.remove(key);
		data.craftingTableLimits.remove(key);
		data.setDirty();
		serverPlayer.sendSystemMessage(Component.literal("Garanti s\u00fcresi doldu."));
	}

	public static void tickFurnace(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity) {
		if (!(state.getBlock() instanceof AbstractFurnaceBlock) || (!state.is(Blocks.FURNACE) && !state.is(Blocks.BLAST_FURNACE))) {
			return;
		}

		long key = pos.asLong();
		int cookingProgress = ((AbstractFurnaceBlockEntityAccessor)blockEntity).minecraftmod$getCookingTimer();
		FurnaceOutputSnapshot output = FurnaceOutputSnapshot.from(blockEntity);
		Map<Long, FurnaceOutputSnapshot> outputMap = FURNACE_OUTPUTS.computeIfAbsent(level, ignored -> new HashMap<>());
		FurnaceOutputSnapshot previousOutput = outputMap.put(key, output);

		boolean smeltedItem = previousOutput != null && output.isIncreaseFrom(previousOutput);
		boolean waterCooling = isWaterCooling(level, pos);
		AnnoyanceSavedData data = data(level);
		Map<Long, Integer> heatMap = data.furnaceHeat;
		int heat = heatMap.getOrDefault(key, 0);
		if (smeltedItem) {
			heat += state.is(Blocks.BLAST_FURNACE) ? 2 : 1;
		} else if (cookingProgress <= 0 && level.getGameTime() % 20L == 0L) {
			heat -= 1;
		}
		if (waterCooling) {
			heat -= 4;
		}
		heat = Math.max(0, heat);
		heatMap.put(key, heat);

		int limit = data.furnaceLimits
			.computeIfAbsent(key, ignored -> 20 + level.random.nextInt(45));
		data.setDirty();
		if (heat >= limit) {
			igniteAround(level, pos);
		}
		if (heat >= limit + 100) {
			heatMap.remove(key);
			data.furnaceLimits.remove(key);
			data.setDirty();
			FURNACE_OUTPUTS.getOrDefault(level, Map.of()).remove(key);
			level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5.0F, ServerLevel.ExplosionInteraction.BLOCK);
		}
	}

	public static void onBlockStateChanged(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
		if (!(level instanceof ServerLevel serverLevel) || oldState == newState) {
			return;
		}

		long key = pos.asLong();
		AnnoyanceSavedData data = data(serverLevel);
		boolean changed = false;
		if (oldState.is(Blocks.CRAFTING_TABLE) && !newState.is(Blocks.CRAFTING_TABLE)) {
			changed |= data.craftingTableUses.remove(key) != null;
			changed |= data.craftingTableLimits.remove(key) != null;
		}
		if (isTrackedFurnace(oldState) && !isTrackedFurnace(newState)) {
			changed |= data.furnaceHeat.remove(key) != null;
			changed |= data.furnaceLimits.remove(key) != null;
			Map<Long, FurnaceOutputSnapshot> outputs = FURNACE_OUTPUTS.get(serverLevel);
			if (outputs != null) {
				outputs.remove(key);
			}
		}
		if (isDiamondOre(oldState) && !isDiamondOre(newState)) {
			changed |= data.diamondMoves.remove(key) != null;
		}
		if (changed) {
			data.setDirty();
		}
	}

	public static boolean hasAnvilTax(Player player) {
		return count(player, Items.EMERALD_BLOCK) >= 1 || count(player, Items.DIAMOND) >= 1 || count(player, Items.EMERALD) >= 3;
	}

	public static void takeAnvilTax(Player player) {
		if (remove(player, Items.EMERALD_BLOCK, 1) || remove(player, Items.DIAMOND, 1) || remove(player, Items.EMERALD, 3)) {
			player.displayClientMessage(Component.literal("Too Expensive! yerine vergi al\u0131nd\u0131."), false);
		}
	}

	private static void tickEyeGps(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}

		for (ServerPlayer player : level.players()) {
			if (!player.getMainHandItem().is(Items.ENDER_EYE) && !player.getOffhandItem().is(Items.ENDER_EYE)) {
				continue;
			}

			BlockPos stronghold = level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, player.blockPosition(), 100, false);
			if (stronghold == null || player.blockPosition().distSqr(stronghold) > 320.0D * 320.0D) {
				continue;
			}

			if (!level.canSeeSky(player.blockPosition())) {
				player.displayClientMessage(Component.literal("GPS sinyali kayboldu."), true);
				continue;
			}

			int pick = player.getRandom().nextInt(3);
			if (pick == 0) {
				player.displayClientMessage(Component.literal("200 metre sonra sa\u011fa d\u00f6n\u00fcn."), true);
			} else if (pick == 1) {
				player.displayClientMessage(Component.literal("Rota yeniden hesaplan\u0131yor..."), true);
			} else {
				player.displayClientMessage(Component.literal("M\u00fcmk\u00fcn oldu\u011funda U d\u00f6n\u00fc\u015f\u00fc yap\u0131n."), true);
			}
		}
	}

	private static boolean isWaterCooling(ServerLevel level, BlockPos pos) {
		for (BlockPos check : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (level.getFluidState(check).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	private static void igniteAround(ServerLevel level, BlockPos pos) {
		for (BlockPos check : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
			if (level.isEmptyBlock(check) && Blocks.FIRE.defaultBlockState().canSurvive(level, check)) {
				level.setBlock(check, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private static void moveNearbyDiamondOres(ServerLevel level) {
		AnnoyanceSavedData data = data(level);
		Map<Long, Integer> moves = data.diamondMoves;
		Set<Long> movedThisTick = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			AABB area = player.getBoundingBox().inflate(5.0D);
			BlockPos.betweenClosedStream(
				(int)Math.floor(area.minX), (int)Math.floor(area.minY), (int)Math.floor(area.minZ),
				(int)Math.floor(area.maxX), (int)Math.floor(area.maxY), (int)Math.floor(area.maxZ)
			).forEach(pos -> tryMoveDiamond(level, player, data, moves, movedThisTick, pos.immutable()));
		}
	}

	private static void tryMoveDiamond(ServerLevel level, ServerPlayer player, AnnoyanceSavedData data, Map<Long, Integer> moves, Set<Long> movedThisTick, BlockPos pos) {
		long key = pos.asLong();
		if (movedThisTick.contains(key)) {
			return;
		}

		BlockState state = level.getBlockState(pos);
		int moveCount = moves.getOrDefault(key, 0);
		if (moveCount >= MAX_DIAMOND_MOVES || !isDiamondOre(state) || !isPlayerLookingAt(player, pos)) {
			return;
		}

		BlockPos[] candidates = orderedAirMoveCandidates(player, pos);
		for (BlockPos candidate : candidates) {
			if (level.isEmptyBlock(candidate)) {
				level.setBlock(candidate, state, Block.UPDATE_ALL);
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				moves.remove(key);
				long candidateKey = candidate.asLong();
				moves.put(candidateKey, moveCount + 1);
				data.setDirty();
				movedThisTick.add(candidateKey);
				return;
			}
		}
	}

	private static boolean isDiamondOre(BlockState state) {
		return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
	}

	private static boolean isTrackedFurnace(BlockState state) {
		return state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE);
	}

	private static boolean isPlayerLookingAt(ServerPlayer player, BlockPos pos) {
		if (player.blockPosition().distSqr(pos) > 5.0D * 5.0D) {
			return false;
		}

		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getLookAngle().scale(5.0D));
		BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
	}

	private static BlockPos[] orderedAirMoveCandidates(ServerPlayer player, BlockPos pos) {
		double dx = pos.getX() + 0.5D - player.getX();
		double dz = pos.getZ() + 0.5D - player.getZ();
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx >= 0.0D
				? new BlockPos[] {pos.east(), pos.above(), pos.north(), pos.south(), pos.west(), pos.below()}
				: new BlockPos[] {pos.west(), pos.above(), pos.north(), pos.south(), pos.east(), pos.below()};
		}
		return dz >= 0.0D
			? new BlockPos[] {pos.south(), pos.above(), pos.east(), pos.west(), pos.north(), pos.below()}
			: new BlockPos[] {pos.north(), pos.above(), pos.east(), pos.west(), pos.south(), pos.below()};
	}

	private static int count(Player player, Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static boolean remove(Player player, Item item, int amount) {
		if (count(player, item) < amount) {
			return false;
		}
		int remaining = amount;
		for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.is(item)) {
				continue;
			}
			int take = Math.min(remaining, stack.getCount());
			stack.shrink(take);
			remaining -= take;
		}
		return true;
	}

	private record FurnaceOutputSnapshot(Item item, int count, DataComponentPatch components) {
		private static FurnaceOutputSnapshot from(AbstractFurnaceBlockEntity blockEntity) {
			ItemStack output = blockEntity.getItem(2);
			return output.isEmpty()
				? new FurnaceOutputSnapshot(null, 0, DataComponentPatch.EMPTY)
				: new FurnaceOutputSnapshot(output.getItem(), output.getCount(), output.getComponentsPatch());
		}

		private boolean isIncreaseFrom(FurnaceOutputSnapshot previous) {
			return this.item != null
				&& (
					previous.item == null && this.count > 0
					|| this.item == previous.item && this.components.equals(previous.components) && this.count > previous.count
				);
		}
	}

	private static AnnoyanceSavedData data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(AnnoyanceSavedData.TYPE);
	}

	private static final class AnnoyanceSavedData extends SavedData {
		private static final Codec<Map<Long, Integer>> LONG_INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(
			AnnoyanceSavedData::decodeLongMap,
			AnnoyanceSavedData::encodeLongMap
		);
		private static final Codec<AnnoyanceSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LONG_INT_MAP_CODEC.fieldOf("crafting_table_uses").forGetter(data -> data.craftingTableUses),
			LONG_INT_MAP_CODEC.fieldOf("crafting_table_limits").forGetter(data -> data.craftingTableLimits),
			LONG_INT_MAP_CODEC.fieldOf("furnace_heat").forGetter(data -> data.furnaceHeat),
			LONG_INT_MAP_CODEC.fieldOf("furnace_limits").forGetter(data -> data.furnaceLimits),
			LONG_INT_MAP_CODEC.fieldOf("diamond_moves").forGetter(data -> data.diamondMoves)
		).apply(instance, AnnoyanceSavedData::new));
		private static final SavedDataType<AnnoyanceSavedData> TYPE = new SavedDataType<>(
			"minecraftmod_world_annoyance",
			AnnoyanceSavedData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
		);

		private final Map<Long, Integer> craftingTableUses;
		private final Map<Long, Integer> craftingTableLimits;
		private final Map<Long, Integer> furnaceHeat;
		private final Map<Long, Integer> furnaceLimits;
		private final Map<Long, Integer> diamondMoves;

		private AnnoyanceSavedData() {
			this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
		}

		private AnnoyanceSavedData(
			Map<Long, Integer> craftingTableUses,
			Map<Long, Integer> craftingTableLimits,
			Map<Long, Integer> furnaceHeat,
			Map<Long, Integer> furnaceLimits,
			Map<Long, Integer> diamondMoves
		) {
			this.craftingTableUses = new HashMap<>(craftingTableUses);
			this.craftingTableLimits = new HashMap<>(craftingTableLimits);
			this.furnaceHeat = new HashMap<>(furnaceHeat);
			this.furnaceLimits = new HashMap<>(furnaceLimits);
			this.diamondMoves = new HashMap<>(diamondMoves);
		}

		private static Map<Long, Integer> decodeLongMap(Map<String, Integer> raw) {
			Map<Long, Integer> decoded = new HashMap<>();
			raw.forEach((key, value) -> {
				try {
					decoded.put(Long.parseLong(key), value);
				} catch (NumberFormatException ignored) {
				}
			});
			return decoded;
		}

		private static Map<String, Integer> encodeLongMap(Map<Long, Integer> raw) {
			Map<String, Integer> encoded = new HashMap<>();
			raw.forEach((key, value) -> encoded.put(Long.toString(key), value));
			return encoded;
		}
	}
}
