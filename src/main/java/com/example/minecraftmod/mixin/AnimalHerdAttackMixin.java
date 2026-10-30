package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AnimalAttackRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class AnimalHerdAttackMixin {
	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void minecraftmod$alertNearbyAnimals(
		ServerLevel level,
		DamageSource damageSource,
		float amount,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!Boolean.TRUE.equals(cir.getReturnValue())) {
			return;
		}

		LivingEntity victim = (LivingEntity)(Object)this;
		if (!(victim instanceof Animal animal) || !(damageSource.getEntity() instanceof Player player)) {
			return;
		}

		AnimalAttackRules.triggerHerdAttack(level, animal, player);
	}
}
