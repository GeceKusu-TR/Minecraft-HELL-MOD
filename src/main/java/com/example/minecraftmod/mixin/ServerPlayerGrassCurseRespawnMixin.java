package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GrassCurseRules;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerGrassCurseRespawnMixin {
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void minecraftmod$restoreCursedGrass(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
		GrassCurseRules.transferCursedGrassAfterRespawn(oldPlayer, (ServerPlayer)(Object)this);
	}
}
