package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SpearRules;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
	@Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockSpearExhaustion(float exhaustion, CallbackInfo ci) {
		if (SpearRules.isLunging()) {
			ci.cancel();
		}
	}
}
