package com.example.minecraftmod.mixin;

import com.example.minecraftmod.WitherRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerWitherShieldMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void minecraftmod$countWitherShieldHits(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
		WitherRules.handleWitherShieldHit((Player)(Object)this, damageSource);
	}
}
