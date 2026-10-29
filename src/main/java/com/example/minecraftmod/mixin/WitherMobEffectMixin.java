package com.example.minecraftmod.mixin;

import com.example.minecraftmod.WitherRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.WitherMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherMobEffect.class)
public abstract class WitherMobEffectMixin {
	@Inject(method = "applyEffectTick", at = @At("TAIL"))
	private void minecraftmod$makeWitherEffectDeadlier(ServerLevel level, LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
		WitherRules.applyExtraWitherEffectDamage(level, entity, amplifier);
	}
}
