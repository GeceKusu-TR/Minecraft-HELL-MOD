package com.example.minecraftmod.mixin;

import com.example.minecraftmod.WitherRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
	@ModifyConstant(method = "createAttributes", constant = @Constant(doubleValue = 300.0D))
	private static double minecraftmod$doubleWitherHealth(double original) {
		return WitherRules.maxHealth(original);
	}

	@ModifyConstant(method = "registerGoals", constant = @Constant(intValue = 40))
	private int minecraftmod$doubleWitherAttackSpeed(int original) {
		return WitherRules.attackInterval(original);
	}

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	private void minecraftmod$tickWitherPhaseThree(ServerLevel level, CallbackInfo ci) {
		WitherBoss wither = (WitherBoss)(Object)this;
		WitherRules.tickPhaseThree(level, wither);
	}

	@Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
	private void minecraftmod$dropSpeedrunTaxLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit, CallbackInfo ci) {
		WitherRules.dropSpeedrunTaxLoot((WitherBoss)(Object)this, level);
	}
}
