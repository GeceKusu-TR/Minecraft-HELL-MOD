package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SleepRules;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSleepMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void minecraftmod$enforceSleepWiggles(CallbackInfo ci) {
		SleepRules.tick((ServerPlayer)(Object)this);
	}
}
