package com.example.minecraftmod.client.mixin;

import com.example.minecraftmod.client.DeathMessageHudState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
	@Shadow
	@Final
	@Mutable
	private Component causeOfDeath;

	protected DeathScreenMixin(Component title) { super(title); }

	@Inject(method = "render", at = @At("HEAD"))
	private void minecraftmod$hideVanillaDeathCause(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		causeOfDeath = null;
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void minecraftmod$renderDeathMessage(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		Component message = DeathMessageHudState.message();
		if (message != null) {
		List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(message, Math.max(80, this.width - 24));
		int lineHeight = 10;
		int startY = this.height / 2 - 50 - (lines.size() - 1) * lineHeight;
		for (int index = 0; index < lines.size(); index++) {
			guiGraphics.drawCenteredString(this.font, lines.get(index), this.width / 2, startY + index * lineHeight, 0xFFFF5555);
		}
		}
	}
}
