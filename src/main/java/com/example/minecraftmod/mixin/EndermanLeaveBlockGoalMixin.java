package com.example.minecraftmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public abstract class EndermanLeaveBlockGoalMixin {
	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$disableVanillaBlockPlacement(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
