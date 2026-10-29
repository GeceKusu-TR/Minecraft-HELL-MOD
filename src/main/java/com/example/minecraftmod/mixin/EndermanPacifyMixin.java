package com.example.minecraftmod.mixin;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderMan.class)
public abstract class EndermanPacifyMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$ignorePlayerTargets(LivingEntity target, CallbackInfo ci) {
		if (target instanceof Player) {
			ci.cancel();
		}
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void minecraftmod$forgetPlayerTargets(CallbackInfo ci) {
		EnderMan enderman = (EnderMan)(Object)this;
		if (enderman.getTarget() instanceof Player) {
			enderman.setTarget(null);
		}
	}

	@Redirect(
		method = "hurtServer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z")
	)
	private boolean minecraftmod$letArrowsHitEndermen(DamageSource damageSource, TagKey<DamageType> tagKey) {
		if (damageSource.getDirectEntity() instanceof Arrow) {
			return false;
		}
		return damageSource.is(tagKey);
	}
}
