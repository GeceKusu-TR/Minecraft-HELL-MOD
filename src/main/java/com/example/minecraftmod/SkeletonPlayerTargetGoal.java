package com.example.minecraftmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

import java.util.EnumSet;

public final class SkeletonPlayerTargetGoal extends Goal {
	private final AbstractSkeleton skeleton;
	private final double range;
	private ServerPlayer target;

	public SkeletonPlayerTargetGoal(AbstractSkeleton skeleton, double range) {
		this.skeleton = skeleton;
		this.range = range;
		this.setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		this.target = findTarget();
		return this.target != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.target == null || !SkeletonTargetingRules.isValidTarget(this.target)) {
			return false;
		}

		if (this.skeleton.distanceToSqr(this.target) > this.range * this.range) {
			return false;
		}

		return SkeletonTargetingRules.canSeeTarget(this.skeleton, this.target);
	}

	@Override
	public void start() {
		if (this.target != null) {
			this.skeleton.setTarget(this.target);
		}
	}

	@Override
	public void tick() {
		ServerPlayer freshTarget = findTarget();
		if (freshTarget != null) {
			this.target = freshTarget;
			this.skeleton.setTarget(freshTarget);
		}
	}

	@Override
	public void stop() {
		if (this.skeleton.getTarget() == this.target) {
			this.skeleton.setTarget(null);
		}

		this.target = null;
	}

	private ServerPlayer findTarget() {
		if (!(this.skeleton.level() instanceof ServerLevel serverLevel)) {
			return null;
		}

		return SkeletonTargetingRules.findNearestVisiblePlayer(serverLevel, this.skeleton);
	}
}
