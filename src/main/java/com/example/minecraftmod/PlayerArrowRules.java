package com.example.minecraftmod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PlayerArrowRules {
	private static final double TARGET_RANGE = 80.0D;
	private static final double HOMING_ARROW_SPEED = 0.75D;
	private static final Map<AbstractArrow, UUID> HOMING_TARGETS = new WeakHashMap<>();

	private PlayerArrowRules() {
	}

	public static void homePlayerArrow(AbstractArrow arrow) {
		if (arrow.level().isClientSide() || !(arrow.level() instanceof ServerLevel level) || !(arrow.getOwner() instanceof ServerPlayer player)) {
			return;
		}

		if (!(arrow instanceof Arrow)) {
			return;
		}

		LivingEntity target = getOrFindTarget(level, arrow, player);
		if (target == null) {
			arrow.setNoGravity(false);
			return;
		}

		Vec3 toTarget = target.getEyePosition().subtract(arrow.position());
		double distance = toTarget.length();
		if (distance <= 1.0E-4D) {
			arrow.setNoGravity(false);
			return;
		}

		arrow.setNoGravity(true);
		arrow.setDeltaMovement(toTarget.scale(HOMING_ARROW_SPEED / distance));
	}

	private static LivingEntity getOrFindTarget(ServerLevel level, AbstractArrow arrow, ServerPlayer player) {
		UUID targetId = HOMING_TARGETS.get(arrow);
		if (targetId != null) {
			Entity entity = level.getEntityInAnyDimension(targetId);
			if (entity instanceof LivingEntity target && isValidTargetForArrow(player, arrow, target)) {
				return target;
			}
			HOMING_TARGETS.remove(arrow);
		}

		LivingEntity target = findTarget(level, arrow, player);
		if (target != null) {
			HOMING_TARGETS.put(arrow, target.getUUID());
		}
		return target;
	}

	private static LivingEntity findTarget(ServerLevel level, AbstractArrow arrow, ServerPlayer player) {
		Vec3 origin = arrow.position();
		LivingEntity bestTarget = null;
		double nearestDistance = TARGET_RANGE * TARGET_RANGE;
		AABB searchBox = arrow.getBoundingBox().inflate(TARGET_RANGE);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> isValidTargetForArrow(player, arrow, target))) {
			double distance = target.distanceToSqr(origin);
			if (distance > nearestDistance) {
				continue;
			}

			if (distance <= nearestDistance) {
				nearestDistance = distance;
				bestTarget = target;
			}
		}

		return bestTarget;
	}

	private static boolean isValidTarget(ServerPlayer owner, LivingEntity target) {
		if (!target.isAlive() || target == owner || target.isSpectator()) {
			return false;
		}
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return false;
		}
		return target.canBeSeenByAnyone();
	}

	private static boolean isValidTargetForArrow(ServerPlayer owner, AbstractArrow arrow, LivingEntity target) {
		return target.level() == arrow.level()
			&& arrow.distanceToSqr(target) <= TARGET_RANGE * TARGET_RANGE
			&& owner.hasLineOfSight(target)
			&& isValidTarget(owner, target);
	}
}
