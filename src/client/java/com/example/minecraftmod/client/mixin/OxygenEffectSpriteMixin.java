package com.example.minecraftmod.client.mixin;

import com.example.minecraftmod.OxygenEffects;
import com.example.minecraftmod.OxygenMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.client.gui.Gui;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public class OxygenEffectSpriteMixin {
	@Inject(method = "getMobEffectSprite", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$forceOxygenEffectSprite(Holder<MobEffect> effect, CallbackInfoReturnable<Identifier> cir) {
		if (effect.value() instanceof OxygenMobEffect) {
			cir.setReturnValue(OxygenEffects.OXYGENLESS_SPRITE);
		}
	}
}
