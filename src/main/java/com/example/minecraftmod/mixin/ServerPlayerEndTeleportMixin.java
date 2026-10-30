package com.example.minecraftmod.mixin;

import com.example.minecraftmod.EndCompletionRules;
import com.example.minecraftmod.EndExperienceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEndTeleportMixin {
	@Unique
	private boolean minecraftmod$endReturnPending;
	@Unique
	private ServerLevel minecraftmod$endReturnSourceLevel;
	@Unique
	private BlockPos minecraftmod$endReturnSourcePosition;

	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$delayFirstEndTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		minecraftmod$endReturnPending = player.level().dimension() == Level.END
			&& transition.newLevel().dimension() == Level.OVERWORLD;
		minecraftmod$endReturnSourceLevel = minecraftmod$endReturnPending ? (ServerLevel) player.level() : null;
		minecraftmod$endReturnSourcePosition = minecraftmod$endReturnPending ? player.blockPosition() : null;
		if (EndExperienceRules.shouldDelayEndTeleport(player, transition)) {
			cir.setReturnValue(player);
		}
	}

	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("RETURN"))
	private void minecraftmod$handleEndCompletion(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
		ServerPlayer player = cir.getReturnValue();
		if (minecraftmod$endReturnPending && player != null && player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) {
			EndCompletionRules.onEndReturn(player, minecraftmod$endReturnSourceLevel, minecraftmod$endReturnSourcePosition);
		}
		minecraftmod$endReturnPending = false;
		minecraftmod$endReturnSourceLevel = null;
		minecraftmod$endReturnSourcePosition = null;
	}
}
