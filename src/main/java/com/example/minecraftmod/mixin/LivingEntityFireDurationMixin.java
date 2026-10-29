package com.example.minecraftmod.mixin;

import com.example.minecraftmod.FireRules;
import com.example.minecraftmod.UndeadRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFireDurationMixin {
	@Inject(method = "hurtServer", at = @At("TAIL"))
	private void minecraftmod$extendBurnDuration(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(cir.getReturnValue())) {
			return;
		}

		LivingEntity livingEntity = (LivingEntity)(Object)this;
		if (livingEntity.isInWater() || livingEntity.fireImmune() || UndeadRules.isProtectedUndead(livingEntity)) {
			return;
		}

		// Burning damage must not restart its own countdown.
		if (!FireRules.isFireDamage(damageSource)
			|| damageSource.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)) {
			return;
		}

		int desiredFireTicks = FireRules.fireTicksFor(level);
		if (desiredFireTicks > livingEntity.getRemainingFireTicks()) {
			livingEntity.setRemainingFireTicks(desiredFireTicks);
		}
	}
}
