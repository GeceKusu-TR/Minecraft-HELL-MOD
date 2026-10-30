package com.example.minecraftmod.client.mixin;

import com.example.minecraftmod.client.HardModeHudState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HardModeHudMixin {
	private static final Component HARD_MODE_LABEL = Component.literal("[1.5x HARD]");
	private static final int HARD_MODE_RED = 0xFFFF3333;

	@Inject(method = "render", at = @At("TAIL"))
	private void minecraftmod$renderHardModeLabel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || !HardModeHudState.shouldRender()) {
			return;
		}

		Font font = minecraft.font;
		guiGraphics.drawString(font, HardModeHudState.label(), 4, 4, HARD_MODE_RED, true);
	}
}
