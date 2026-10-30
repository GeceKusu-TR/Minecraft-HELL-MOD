package com.example.minecraftmod.client.mixin;

import com.example.minecraftmod.client.HardModeHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.DebugScreenOverlay")
public abstract class HardModeDebugOverlayMixin {
	private static final Component HARD_MODE_LABEL = Component.literal("[1.5x HARD]");
	private static final int HARD_MODE_RED = 0xFFFF3333;

	@Inject(method = "render", at = @At("TAIL"))
	private void minecraftmod$renderHardModeLabelAboveDebugText(GuiGraphics guiGraphics, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null && HardModeHudState.shouldRender()) {
			guiGraphics.drawString(minecraft.font, HardModeHudState.label(), 4, 4, HARD_MODE_RED, true);
		}
	}
}
