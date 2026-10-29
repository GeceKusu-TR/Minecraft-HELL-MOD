package com.example.minecraftmod.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void minecraftmod$removeNetherFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (level.dimension() != Level.NETHER) {
			return;
		}

		float clearDistance = Math.max(renderDistance, 512.0F);
		fogData.environmentalStart = clearDistance;
		fogData.environmentalEnd = clearDistance;
		fogData.skyEnd = clearDistance;
		fogData.cloudEnd = clearDistance;
	}
}
