package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OxygenRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class UnderwaterBreathMixin {
	@Redirect(
		method = "baseTick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;shouldTakeDrowningDamage()Z")
	)
	private boolean minecraftmod$convertDrowningDamage(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
			return minecraftmod$vanillaDrowningDamage(entity);
		}

		if (!OxygenRules.isUnderwater(player)) {
			return minecraftmod$vanillaDrowningDamage(entity);
		}

		return false;
	}

	@Unique
	private boolean minecraftmod$vanillaDrowningDamage(LivingEntity entity) {
		return entity.getAirSupply() <= -20;
	}
}
