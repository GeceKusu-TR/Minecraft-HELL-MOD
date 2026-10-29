package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SkeletonTargetingRules;
import com.example.minecraftmod.PlayerArrowRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class SkeletonArrowHomingMixin {
	private static final double MINECRAFTMOD_SKELETON_HOMING_ARROW_SPEED = 0.75D;

	@Inject(method = "tick", at = @At("HEAD"))
	private void minecraftmod$homeSkeletonArrow(CallbackInfo ci) {
		AbstractArrow arrow = (AbstractArrow)(Object)this;
		if (arrow.level().isClientSide()) {
			return;
		}

		Entity owner = arrow.getOwner();
		if (owner instanceof ServerPlayer) {
			PlayerArrowRules.homePlayerArrow(arrow);
			return;
		}

		if (!(owner instanceof AbstractSkeleton skeleton)) {
			return;
		}

		if (!(skeleton.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		ServerPlayer target = skeleton.getTarget() instanceof ServerPlayer player && SkeletonTargetingRules.canSeeTarget(skeleton, player)
			? player
			: SkeletonTargetingRules.findNearestVisiblePlayer(serverLevel, skeleton);
		if (target == null) {
			return;
		}

		Vec3 toTarget = target.getEyePosition().subtract(arrow.position());
		double distance = toTarget.length();
		if (distance <= 1.0E-4D) {
			return;
		}

		arrow.setNoGravity(true);
		arrow.setDeltaMovement(toTarget.scale(MINECRAFTMOD_SKELETON_HOMING_ARROW_SPEED / distance));
	}
}
