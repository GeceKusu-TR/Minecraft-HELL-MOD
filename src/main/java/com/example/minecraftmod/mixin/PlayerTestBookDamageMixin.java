package com.example.minecraftmod.mixin;

import com.example.minecraftmod.TestBookRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerTestBookDamageMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$preventDamageWhileReadingTestBooks(ServerLevel level, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
		if ((Object)this instanceof ServerPlayer player && TestBookRules.isReadingRequiredBooks(player)) {
			cir.setReturnValue(false);
		}
	}
}
