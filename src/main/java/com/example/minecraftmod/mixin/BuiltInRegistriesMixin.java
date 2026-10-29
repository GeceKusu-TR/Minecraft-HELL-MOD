package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OxygenEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesMixin {
	@Inject(method = "freeze", at = @At("HEAD"))
	private static void minecraftmod$registerModEntriesBeforeFreeze(CallbackInfo ci) {
		OxygenEffects.bootstrap();
	}
}
