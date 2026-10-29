package com.example.minecraftmod.mixin;

import com.example.minecraftmod.FishingRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class FishingTridentMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void minecraftmod$homeFishingTrident(CallbackInfo ci) {
		FishingRules.tickFishingTrident((ThrownTrident)(Object)this);
	}

	@Redirect(
		method = "onHitEntity",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
	)
	private boolean minecraftmod$setFishingTridentDamage(Entity hitEntity, DamageSource damageSource, float amount) {
		return FishingRules.hurtWithFishingTrident((ThrownTrident)(Object)this, hitEntity, damageSource, amount);
	}
}
