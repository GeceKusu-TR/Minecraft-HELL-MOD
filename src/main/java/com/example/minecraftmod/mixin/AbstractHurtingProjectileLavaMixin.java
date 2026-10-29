package com.example.minecraftmod.mixin;

import com.example.minecraftmod.LavaFireRules;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileLavaMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void minecraftmod$trailGhastLavaFireball(CallbackInfo ci) {
		Object projectile = this;
		if (projectile instanceof LargeFireball largeFireball) {
			LavaFireRules.tickGhastLavaFireball(largeFireball);
		}
	}
}
