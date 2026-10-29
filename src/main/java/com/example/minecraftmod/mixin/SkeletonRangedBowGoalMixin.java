package com.example.minecraftmod.mixin;

import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedBowAttackGoal.class)
public abstract class SkeletonRangedBowGoalMixin {
	@Shadow private Monster mob;
	@Shadow @Final @Mutable private float attackRadiusSqr;

	@Inject(method = "<init>(Lnet/minecraft/world/entity/monster/Monster;DIF)V", at = @At("RETURN"))
	private void minecraftmod$extendSkeletonRange(Monster mob, double speedModifier, int attackIntervalMin, float attackRadius, CallbackInfo ci) {
		if (this.mob instanceof AbstractSkeleton) {
			attackRadiusSqr = 50.0F * 50.0F;
		}
	}
}
