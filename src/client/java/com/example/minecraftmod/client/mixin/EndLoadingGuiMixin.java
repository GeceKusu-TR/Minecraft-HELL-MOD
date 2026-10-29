package com.example.minecraftmod.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class EndLoadingGuiMixin {
	@Shadow
	private Component title;

	@Inject(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderTitle(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
	)
	private void minecraftmod$renderBlackEndLoadingBackground(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!minecraftmod$isEndLoadingTitle()) {
			return;
		}

		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
	}

	private boolean minecraftmod$isEndLoadingTitle() {
		if (title == null) {
			return false;
		}

		String text = title.getString();
		return text.equals("The End y\u00fckleniyor...")
			|| text.equals("Shader'lar derleniyor...")
			|| text.equals("Enderman'lar haz\u0131rlan\u0131yor...")
			|| text.equals("Ejderha uyand\u0131r\u0131l\u0131yor...")
			|| text.equals("Gereksiz bekleme ekleniyor...");
	}
}
