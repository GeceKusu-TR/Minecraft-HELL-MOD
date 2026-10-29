package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OxygenRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class TurtleShellAirDrainMixin {
	@Inject(method = "baseTick", at = @At("TAIL"))
	private void minecraftmod$drainAirFasterWithTurtleShell(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity)(Object)this;
		if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
			return;
		}

		if (!OxygenRules.isUnderwater(player) || !OxygenRules.hasTurtleShellHelmet(player)) {
			return;
		}

		if (player.canBreatheUnderwater()) {
			return;
		}

		player.setAirSupply(player.getAirSupply() - 1);
	}
}
