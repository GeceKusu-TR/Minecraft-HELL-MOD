package com.example.minecraftmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SnowGolemProtectionMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$ignoreLivingAttackers(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object)this instanceof net.minecraft.world.entity.animal.golem.SnowGolem)) {
			return;
		}

		Entity directEntity = source.getDirectEntity();
		Entity causingEntity = source.getEntity();
		if (directEntity instanceof LivingEntity || causingEntity instanceof LivingEntity) {
			cir.setReturnValue(false);
		}
	}
}
