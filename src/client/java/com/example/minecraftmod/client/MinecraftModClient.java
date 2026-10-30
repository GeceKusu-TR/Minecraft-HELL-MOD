package com.example.minecraftmod.client;

import com.example.minecraftmod.CompletionHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MinecraftModClient implements ClientModInitializer {
	private static final Component HARD_MODE_LABEL = Component.literal("[1.5x HARD]");
	private static final int HARD_MODE_RED = 0xFFFF3333;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(CompletionHudPayload.TYPE, (payload, context) ->
			context.client().execute(() -> HardModeHudState.setCompleted(payload.completed(), payload.hardcoreEscalated())));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HardModeHudState.setCompleted(false, false));
		HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.level != null && HardModeHudState.shouldRender()) {
				guiGraphics.drawString(minecraft.font, HardModeHudState.label(), 4, 4, HARD_MODE_RED, true);
			}
		});
	}
}
