package com.example.minecraftmod.client;

import net.minecraft.network.chat.Component;

public final class DeathMessageHudState {
	private static Component message;

	private DeathMessageHudState() {
	}

	public static void setMessage(String value) {
		message = Component.literal(value);
	}

	public static Component message() {
		return message;
	}
}
