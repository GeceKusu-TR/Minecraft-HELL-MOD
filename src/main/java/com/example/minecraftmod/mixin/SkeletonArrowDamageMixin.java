package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SkeletonArrowRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class SkeletonArrowDamageMixin {
	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$limitSkeletonArrowDamage(EntityHitResult hitResult, CallbackInfo ci) {
		if (SkeletonArrowRules.handleSkeletonArrowHit((AbstractArrow)(Object)this, hitResult)) {
			ci.cancel();
		}
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void minecraftmod$discardArrowAfterEndermanHit(EntityHitResult hitResult, CallbackInfo ci) {
		AbstractArrow arrow = (AbstractArrow)(Object)this;
		if (arrow instanceof Arrow && hitResult.getEntity() instanceof EnderMan) {
			arrow.discard();
		}
	}

	@Redirect(
			method = "onHitEntity",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
			)
	)
	private boolean minecraftmod$capSkeletonArrowDamage(Entity hitEntity, DamageSource damageSource, float amount) {
		return SkeletonArrowRules.hurtWithSkeletonArrowCap((AbstractArrow)(Object)this, hitEntity, damageSource, amount);
	}
}
