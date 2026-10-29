package com.example.minecraftmod.mixin;

import com.example.minecraftmod.UndeadRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class UndeadFireMixin {
	@Inject(method = "baseTick", at = @At("HEAD"))
	private void minecraftmod$wearHelmetAndExtinguishUndead(CallbackInfo ci) {
		Entity entity = (Entity)(Object)this;
		if (!(entity instanceof LivingEntity livingEntity) || !UndeadRules.isProtectedUndead(entity)) {
			return;
		}

		UndeadRules.ensureLeatherHelmet(livingEntity);
		entity.clearFire();
	}

	@Inject(method = "igniteForSeconds", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockUndeadIgniteForSeconds(float seconds, CallbackInfo ci) {
		if (UndeadRules.isProtectedUndead((Entity)(Object)this)) {
			ci.cancel();
		}
	}

	@Inject(method = "igniteForTicks", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockUndeadIgniteForTicks(int ticks, CallbackInfo ci) {
		if (UndeadRules.isProtectedUndead((Entity)(Object)this)) {
			ci.cancel();
		}
	}

	@Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockUndeadFireTicks(int fireTicks, CallbackInfo ci) {
		if (UndeadRules.isProtectedUndead((Entity)(Object)this)) {
			ci.cancel();
		}
	}

	@Inject(method = "lavaIgnite", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockUndeadLavaIgnite(CallbackInfo ci) {
		if (UndeadRules.isProtectedUndead((Entity)(Object)this)) {
			ci.cancel();
		}
	}

	@Inject(method = "lavaHurt", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockUndeadLavaHurt(CallbackInfo ci) {
		if (UndeadRules.isProtectedUndead((Entity)(Object)this)) {
			ci.cancel();
		}
	}
}
