package com.example.minecraftmod.mixin;

import com.example.minecraftmod.LavaFireRules;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LargeFireball.class)
public abstract class LargeFireballLavaMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$replaceGhastExplosionWithLava(HitResult hitResult, CallbackInfo ci) {
		if (LavaFireRules.handleGhastLavaFireballHit((LargeFireball)(Object)this, hitResult)) {
			ci.cancel();
		}
	}
}
