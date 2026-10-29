package com.example.minecraftmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SkeletonTargetingRules {
	public static final double TARGET_RANGE = 50.0D;

	private SkeletonTargetingRules() {
	}

	public static ServerPlayer findNearestVisiblePlayer(ServerLevel level, AbstractSkeleton skeleton) {
		ServerPlayer nearestPlayer = null;
		double nearestDistance = TARGET_RANGE * TARGET_RANGE;

		AABB searchBox = skeleton.getBoundingBox().inflate(TARGET_RANGE);
		for (ServerPlayer player : level.players()) {
			if (!isValidTarget(player)) {
				continue;
			}
			if (!searchBox.intersects(player.getBoundingBox())) {
				continue;
			}
			if (!canSeeTarget(skeleton, player)) {
				continue;
			}

			double distance = skeleton.distanceToSqr(player);
			if (distance <= nearestDistance) {
				nearestDistance = distance;
				nearestPlayer = player;
			}
		}

		return nearestPlayer;
	}

	public static boolean isValidTarget(ServerPlayer player) {
		return player.isAlive() && !player.isCreative() && !player.isSpectator() && player.canBeSeenByAnyone();
	}

	public static boolean canSeeTarget(AbstractSkeleton skeleton, ServerPlayer player) {
		if (!isValidTarget(player) || skeleton.level() != player.level()) {
			return false;
		}

		Vec3 start = skeleton.getEyePosition();
		Vec3 end = player.getEyePosition();
		return VisibilityRules.canSeeThroughBlocksAndLava(skeleton.level(), skeleton, start, end);
	}
}
