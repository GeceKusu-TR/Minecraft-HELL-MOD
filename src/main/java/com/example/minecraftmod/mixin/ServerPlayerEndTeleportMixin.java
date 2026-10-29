package com.example.minecraftmod.mixin;

import com.example.minecraftmod.EndExperienceRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEndTeleportMixin {
	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$delayFirstEndTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		if (EndExperienceRules.shouldDelayEndTeleport(player, transition)) {
			cir.setReturnValue(player);
		}
	}
}
