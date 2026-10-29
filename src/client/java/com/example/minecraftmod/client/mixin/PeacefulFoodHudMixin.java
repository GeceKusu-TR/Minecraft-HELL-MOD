package com.example.minecraftmod.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class PeacefulFoodHudMixin {
	@Shadow
	@Final
	private static Identifier FOOD_FULL_SPRITE;

	@Shadow
	@Final
	private static Identifier FOOD_FULL_HUNGER_SPRITE;

	@Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$renderOnlyOneFoodIconInPeaceful(GuiGraphics guiGraphics, Player player, int y, int right, CallbackInfo ci) {
		if (player.level().getDifficulty() != Difficulty.PEACEFUL) {
			return;
		}

		Identifier sprite = player.hasEffect(MobEffects.HUNGER) ? FOOD_FULL_HUNGER_SPRITE : FOOD_FULL_SPRITE;
		guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, right - 9, y, 9, 9);
		ci.cancel();
	}
}
