package com.example.minecraftmod.mixin;

import com.example.minecraftmod.EndermanRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderMan.class)
public abstract class EndermanDecorationMixin {
	@Inject(method = "aiStep", at = @At("TAIL"))
	private void minecraftmod$forceWorldDecoration(CallbackInfo ci) {
		EnderMan enderman = (EnderMan)(Object)this;
		if (enderman.level() instanceof ServerLevel serverLevel) {
			EndermanRules.tickBlockLoop(serverLevel, enderman);
		}
	}
}
