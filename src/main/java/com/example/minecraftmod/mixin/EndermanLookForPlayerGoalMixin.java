package com.example.minecraftmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLookForPlayerGoal")
public abstract class EndermanLookForPlayerGoalMixin {
	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$disablePlayerAggroOnSight(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
