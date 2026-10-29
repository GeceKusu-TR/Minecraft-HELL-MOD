package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GhastRules;
import com.example.minecraftmod.NoMobCommands;
import com.example.minecraftmod.PerformanceRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelEntityLimitMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$limitEndEndermen(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		ServerLevel level = (ServerLevel)(Object)this;
		if (!PerformanceRules.canAddEntityToLevel(level, entity)) {
			entity.discard();
			cir.setReturnValue(false);
			return;
		}

		if (GhastRules.shouldBlockEntitySpawn(entity, level)) {
			entity.discard();
			cir.setReturnValue(false);
			return;
		}

		if (NoMobCommands.shouldBlockHostileSpawn(level, entity)) {
			entity.discard();
			cir.setReturnValue(false);
		}
	}
}
