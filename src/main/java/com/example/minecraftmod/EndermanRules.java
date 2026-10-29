package com.example.minecraftmod;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class EndermanRules {
	private static final int BLOCK_HOLD_TICKS = 20;
	private static final int PICKUP_RADIUS = 2;
	private static final int WANDER_COOLDOWN_TICKS = 60;
	private static final int TELEPORT_COOLDOWN_TICKS = 20;
	private static final double WANDER_SPEED = 0.9D;
	private static final Map<EnderMan, Integer> LAST_PICKUP_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<EnderMan, Integer> LAST_WANDER_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Map<EnderMan, Integer> LAST_TELEPORT_TICKS = Collections.synchronizedMap(new WeakHashMap<>());

	private EndermanRules() {
	}

	public static void tickBlockLoop(ServerLevel level, EnderMan Enderman) {
		if (TntRules.isHeldByEndermanTnt(Enderman)) {
			Enderman.getNavigation().stop();
			Enderman.setDeltaMovement(0.0D, 0.0D, 0.0D);
			Enderman.hurtMarked = true;
			return;
		}

		if (isFallingIntoDugHole(level, Enderman)) {
			teleportToSafeSpot(level, Enderman);
			return;
		}

		BlockState carriedBlock = Enderman.getCarriedBlock();
		if (carriedBlock != null) {
			Integer lastPickupTick = LAST_PICKUP_TICKS.get(Enderman);
			if (lastPickupTick == null) {
				LAST_PICKUP_TICKS.put(Enderman, Enderman.tickCount);
				return;
			}

			if (Enderman.tickCount - lastPickupTick >= BLOCK_HOLD_TICKS) {
				Enderman.setCarriedBlock(null);
				LAST_PICKUP_TICKS.remove(Enderman);
			}

			return;
		}

		BlockPos target = findNearbyBlock(level, Enderman.blockPosition(), PICKUP_RADIUS);
		if (target == null) {
			wanderNearby(level, Enderman);
			return;
		}

		if (tryPickupBlock(level, Enderman, target)) {
			LAST_PICKUP_TICKS.put(Enderman, Enderman.tickCount);
		} else {
			wanderNearby(level, Enderman);
		}
	}

	public static void clearForbiddenCarriedDeathDrops(EnderMan enderman) {
		BlockState carriedBlock = enderman.getCarriedBlock();
		if (carriedBlock != null && carriedBlock.is(Blocks.GRASS_BLOCK)) {
			enderman.setCarriedBlock(null);
		}
	}

	private static BlockPos findNearbyBlock(ServerLevel level, BlockPos origin, int radius) {
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		BlockPos bestPos = null;
		int bestScore = Integer.MAX_VALUE;

		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					mutable.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					BlockState state = level.getBlockState(mutable);
					if (!canEndermanTakeBlock(level, mutable, state)) {
						continue;
					}

					int distance = x * x + y * y + z * z;
					int score = blockPriority(state) * 10_000 + belowFootPenalty(x, y, z) + distance;
					if (score < bestScore) {
						bestScore = score;
						bestPos = mutable.immutable();
					}
				}
			}
		}

		return bestPos;
	}

	private static boolean tryPickupBlock(ServerLevel level, EnderMan Enderman, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!canEndermanTakeBlock(level, pos, state)) {
			return false;
		}

		if (!level.removeBlock(pos, false)) {
			return false;
		}

		level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(Enderman, state));
		Enderman.setCarriedBlock(state);
		return true;
	}

	private static boolean canEndermanTakeBlock(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.TNT)) {
			return false;
		}

		if (level.getBlockEntity(pos) != null) {
			return false;
		}

		return state.getDestroySpeed(level, pos) >= 0.0F;
	}

	private static int blockPriority(BlockState state) {
		if (state.is(Blocks.OBSIDIAN)) {
			return 0;
		}

		if (state.is(Blocks.END_STONE)) {
			return 1;
		}

		if (state.is(Blocks.GRASS_BLOCK)) {
			return 2;
		}

		if (state.is(Blocks.DIRT)) {
			return 3;
		}

		if (state.is(Blocks.STONE)) {
			return 4;
		}

		return 5;
	}

	private static int belowFootPenalty(int x, int y, int z) {
		return x == 0 && z == 0 && y < 0 ? 1_000 : 0;
	}

	private static boolean isFallingIntoDugHole(ServerLevel level, EnderMan Enderman) {
		if (Enderman.onGround()) {
			return false;
		}

		BlockPos feet = Enderman.blockPosition();
		BlockPos below = feet.below();
		return level.isEmptyBlock(below) && level.isEmptyBlock(below.below());
	}

	private static void wanderNearby(ServerLevel level, EnderMan Enderman) {
		Integer lastWanderTick = LAST_WANDER_TICKS.get(Enderman);
		if (lastWanderTick != null && Enderman.tickCount - lastWanderTick < WANDER_COOLDOWN_TICKS) {
			return;
		}

		BlockPos destination = findNearbyWalkTarget(level, Enderman);
		if (destination == null) {
			teleportToSafeSpot(level, Enderman);
			return;
		}

		Enderman.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, WANDER_SPEED);
		LAST_WANDER_TICKS.put(Enderman, Enderman.tickCount);
	}

	private static BlockPos findNearbyWalkTarget(ServerLevel level, EnderMan Enderman) {
		BlockPos origin = Enderman.blockPosition();
		for (int attempt = 0; attempt < 16; attempt++) {
			int x = origin.getX() + Enderman.getRandom().nextInt(17) - 8;
			int z = origin.getZ() + Enderman.getRandom().nextInt(17) - 8;
			int y = origin.getY() + Enderman.getRandom().nextInt(7) - 3;
			BlockPos candidate = new BlockPos(x, y, z);
			if (isSafeStandPosition(level, candidate)) {
				return candidate;
			}
		}

		return null;
	}

	private static void teleportToSafeSpot(ServerLevel level, EnderMan Enderman) {
		Integer lastTeleportTick = LAST_TELEPORT_TICKS.get(Enderman);
		if (lastTeleportTick != null && Enderman.tickCount - lastTeleportTick < TELEPORT_COOLDOWN_TICKS) {
			return;
		}

		LAST_TELEPORT_TICKS.put(Enderman, Enderman.tickCount);
		BlockPos destination = findTeleportDestination(level, Enderman);
		if (destination == null) {
			return;
		}

		Enderman.teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
		LAST_TELEPORT_TICKS.put(Enderman, Enderman.tickCount);
	}

	private static BlockPos findTeleportDestination(ServerLevel level, EnderMan Enderman) {
		BlockPos origin = Enderman.blockPosition();
		for (int attempt = 0; attempt < 20; attempt++) {
			int x = origin.getX() + Enderman.getRandom().nextInt(65) - 32;
			int z = origin.getZ() + Enderman.getRandom().nextInt(65) - 32;
			if (!level.hasChunk(x >> 4, z >> 4)) {
				continue;
			}
			BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
			if (isSafeStandPosition(level, surface)) {
				return surface;
			}
		}

		return null;
	}

	private static boolean isSafeStandPosition(ServerLevel level, BlockPos position) {
		BlockPos below = position.below();
		return level.isEmptyBlock(position)
			&& level.isEmptyBlock(position.above())
			&& level.isEmptyBlock(position.above(2))
			&& level.getWorldBorder().isWithinBounds(position)
			&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}
}
