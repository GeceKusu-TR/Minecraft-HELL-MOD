package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SpearRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockLungeExhaustion(float exhaustion, CallbackInfo ci) {
		if (!SpearRules.isLunging()) {
			return;
		}

		Player player = SpearRules.lungePlayer();
		if (player != null && player.level() instanceof ServerLevel level) {
			player.hurtServer(level, player.damageSources().magic(), 1.0F);
		}

		ci.cancel();
	}
}
