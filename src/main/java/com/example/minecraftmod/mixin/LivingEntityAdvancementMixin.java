package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AdvancementRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAdvancementMixin {
	@Inject(method = "die", at = @At("HEAD"))
	private void minecraftmod$grantKillAdvancements(DamageSource damageSource, CallbackInfo ci) {
		Entity killer = damageSource.getEntity();
		if (killer instanceof ServerPlayer serverPlayer) {
			LivingEntity killed = (LivingEntity)(Object)this;
			AdvancementRules.grantEndermanKilled(serverPlayer, killed);
			AdvancementRules.grantWitherKilled(serverPlayer, killed);
		}
	}
}
