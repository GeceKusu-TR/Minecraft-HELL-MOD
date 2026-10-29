package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SnowRules;
import com.example.minecraftmod.DragonRules;
import com.example.minecraftmod.EndPortalRitualRules;
import com.example.minecraftmod.GhastRules;
import com.example.minecraftmod.GrassCurseRules;
import com.example.minecraftmod.PerformanceRules;
import com.example.minecraftmod.WorldAnnoyanceRules;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTimeMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void minecraftmod$keepDayTimeAtNoon(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		ServerLevel serverLevel = (ServerLevel)(Object)this;
		if (serverLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
			serverLevel.setDayTime(GrassCurseRules.forcedTime(serverLevel));
		}
		SnowRules.applyAroundPlayers(serverLevel);
		DragonRules.tick(serverLevel);
		GhastRules.tick(serverLevel);
		EndPortalRitualRules.tick(serverLevel);
		WorldAnnoyanceRules.tick(serverLevel);
		PerformanceRules.enforceEndEndermanLimit(serverLevel);
	}
}
