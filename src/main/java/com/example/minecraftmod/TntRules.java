package com.example.minecraftmod;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TntRules {
	private static final double ENDERMAN_PULL_RADIUS = 50.0D;
	private static final double ENDERMAN_RING_RADIUS = 1.5D;
	private static final int TNT_BLOCK_BREAK_RADIUS = 20;
	private static final int TNT_EXPLOSION_DELAY_TICKS = 20;
	private static final Set<PrimedTnt> PULLED_TNT = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Set<PrimedTnt> BROKEN_TNT = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<EnderMan, PrimedTnt> HELD_ENDERMEN = Collections.synchronizedMap(new WeakHashMap<>());

	private TntRules() {
	}

	public static void tickEndermanTnt(PrimedTnt tnt) {
		if (!(tnt.level() instanceof ServerLevel level)) {
			return;
		}

		if (PULLED_TNT.add(tnt)) {
			pullEndermen(level, tnt);
			tnt.setFuse(TNT_EXPLOSION_DELAY_TICKS);
			return;
		}

		if (tnt.getFuse() <= 1 && BROKEN_TNT.add(tnt)) {
			breakNearbyBlocks(level, tnt);
			releaseHeldEndermen(tnt);
			return;
		}

		holdEndermen(tnt);
	}

	private static void pullEndermen(ServerLevel level, PrimedTnt tnt) {
		AABB area = tnt.getBoundingBox().inflate(ENDERMAN_PULL_RADIUS);
		var endermen = level.getEntitiesOfClass(
			EnderMan.class,
			area,
			enderman -> enderman.isAlive() && enderman.distanceToSqr(tnt) <= ENDERMAN_PULL_RADIUS * ENDERMAN_PULL_RADIUS
		);
		int count = Math.max(1, endermen.size());
		for (int index = 0; index < endermen.size(); index++) {
			EnderMan enderman = endermen.get(index);
			double angle = Math.TAU * index / count;
			double x = tnt.getX() + Math.cos(angle) * ENDERMAN_RING_RADIUS;
			double z = tnt.getZ() + Math.sin(angle) * ENDERMAN_RING_RADIUS;
			enderman.teleportTo(x, tnt.getY(), z);
			enderman.setDeltaMovement(Vec3.ZERO);
			enderman.hurtMarked = true;
			HELD_ENDERMEN.put(enderman, tnt);
		}
	}

	public static boolean shouldIgnoreExplosionForPlayer(Entity explosionSource, Entity target) {
		return explosionSource instanceof PrimedTnt && target instanceof Player;
	}

	public static boolean shouldProtectBlockFromTntExplosion(Entity explosionSource, BlockState state) {
		return explosionSource instanceof PrimedTnt && isOreOrOreStorageBlock(state);
	}

	public static boolean isHeldByEndermanTnt(EnderMan enderman) {
		PrimedTnt tnt = HELD_ENDERMEN.get(enderman);
		if (tnt == null) {
			return false;
		}
		if (tnt.isRemoved() || tnt.getFuse() <= 0) {
			HELD_ENDERMEN.remove(enderman);
			return false;
		}
		return true;
	}

	private static void holdEndermen(PrimedTnt tnt) {
		Map<EnderMan, PrimedTnt> snapshot;
		synchronized (HELD_ENDERMEN) {
			snapshot = new IdentityHashMap<>(HELD_ENDERMEN);
		}

		int count = 0;
		for (Map.Entry<EnderMan, PrimedTnt> entry : snapshot.entrySet()) {
			EnderMan enderman = entry.getKey();
			if (entry.getValue() == tnt && enderman.isAlive() && !enderman.isRemoved() && enderman.level() == tnt.level()) {
				count++;
			}
		}

		int index = 0;
		for (Map.Entry<EnderMan, PrimedTnt> entry : snapshot.entrySet()) {
			EnderMan enderman = entry.getKey();
			if (entry.getValue() != tnt || !enderman.isAlive() || enderman.isRemoved() || enderman.level() != tnt.level()) {
				if (entry.getValue() == tnt) {
					HELD_ENDERMEN.remove(enderman);
				}
				continue;
			}

			double angle = Math.TAU * index / Math.max(1, count);
			double x = tnt.getX() + Math.cos(angle) * ENDERMAN_RING_RADIUS;
			double z = tnt.getZ() + Math.sin(angle) * ENDERMAN_RING_RADIUS;
			enderman.teleportTo(x, tnt.getY(), z);
			enderman.setDeltaMovement(Vec3.ZERO);
			enderman.hurtMarked = true;
			index++;
		}
	}

	private static void releaseHeldEndermen(PrimedTnt tnt) {
		synchronized (HELD_ENDERMEN) {
			HELD_ENDERMEN.entrySet().removeIf(entry -> entry.getValue() == tnt);
		}
	}

	private static void breakNearbyBlocks(ServerLevel level, PrimedTnt tnt) {
		BlockPos center = tnt.blockPosition();
		int radiusSquared = TNT_BLOCK_BREAK_RADIUS * TNT_BLOCK_BREAK_RADIUS;
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int y = -TNT_BLOCK_BREAK_RADIUS; y <= TNT_BLOCK_BREAK_RADIUS; y++) {
			for (int x = -TNT_BLOCK_BREAK_RADIUS; x <= TNT_BLOCK_BREAK_RADIUS; x++) {
				for (int z = -TNT_BLOCK_BREAK_RADIUS; z <= TNT_BLOCK_BREAK_RADIUS; z++) {
					if (x * x + y * y + z * z > radiusSquared) {
						continue;
					}

					mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					BlockState state = level.getBlockState(mutable);
					if (!canTntForceBreak(level, mutable, state)) {
						continue;
					}

					level.destroyBlock(mutable, false, tnt);
				}
			}
		}
	}

	private static boolean canTntForceBreak(ServerLevel level, BlockPos pos, BlockState state) {
		return !state.isAir()
			&& !state.is(Blocks.BEDROCK)
			&& !state.is(Blocks.OBSIDIAN)
			&& !state.is(Blocks.TNT)
			&& !isOreOrOreStorageBlock(state)
			&& state.getDestroySpeed(level, pos) >= 0.0F;
	}

	public static boolean isOreOrOreStorageBlock(BlockState state) {
		return state.is(BlockTags.COAL_ORES)
			|| state.is(BlockTags.COPPER_ORES)
			|| state.is(BlockTags.DIAMOND_ORES)
			|| state.is(BlockTags.EMERALD_ORES)
			|| state.is(BlockTags.GOLD_ORES)
			|| state.is(BlockTags.IRON_ORES)
			|| state.is(BlockTags.LAPIS_ORES)
			|| state.is(BlockTags.REDSTONE_ORES)
			|| state.is(Blocks.NETHER_QUARTZ_ORE)
			|| state.is(Blocks.NETHER_GOLD_ORE)
			|| state.is(Blocks.ANCIENT_DEBRIS)
			|| state.is(Blocks.COAL_BLOCK)
			|| state.is(Blocks.COPPER_BLOCK)
			|| state.is(Blocks.RAW_COPPER_BLOCK)
			|| state.is(Blocks.IRON_BLOCK)
			|| state.is(Blocks.RAW_IRON_BLOCK)
			|| state.is(Blocks.GOLD_BLOCK)
			|| state.is(Blocks.RAW_GOLD_BLOCK)
			|| state.is(Blocks.DIAMOND_BLOCK)
			|| state.is(Blocks.EMERALD_BLOCK)
			|| state.is(Blocks.LAPIS_BLOCK)
			|| state.is(Blocks.REDSTONE_BLOCK)
			|| state.is(Blocks.NETHERITE_BLOCK)
			|| state.is(Blocks.QUARTZ_BLOCK);
	}
}
