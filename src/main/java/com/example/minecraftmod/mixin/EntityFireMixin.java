package com.example.minecraftmod.mixin;

import com.example.minecraftmod.FireRules;
import com.example.minecraftmod.UndeadRules;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityFireMixin {
	@Inject(method = "clearFire", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockDryFireClear(CallbackInfo ci) {
		Entity entity = (Entity)(Object)this;
		if (FireRules.shouldBurnForever(entity) && !entity.isInWater() && !entity.fireImmune() && !UndeadRules.isProtectedUndead(entity)) {
			ci.cancel();
		}
	}

	@Inject(method = "extinguishFire", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockDryFireExtinguish(CallbackInfo ci) {
		Entity entity = (Entity)(Object)this;
		if (FireRules.shouldBurnForever(entity) && !entity.isInWater() && !entity.fireImmune() && !UndeadRules.isProtectedUndead(entity)) {
			ci.cancel();
		}
	}

	@Inject(method = "setRemainingFireTicks", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$keepFireAliveUntilWater(int fireTicks, CallbackInfo ci) {
		Entity entity = (Entity)(Object)this;
		if (FireRules.shouldBurnForever(entity) && fireTicks <= 0 && !entity.isInWater() && !entity.fireImmune() && !UndeadRules.isProtectedUndead(entity)) {
			ci.cancel();
		}
	}
}
