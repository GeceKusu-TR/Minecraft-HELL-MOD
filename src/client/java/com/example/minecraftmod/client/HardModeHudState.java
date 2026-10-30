package com.example.minecraftmod.client;

import net.minecraft.network.chat.Component;

public final class HardModeHudState {
	private static final Component FIRST_CLEAR_LABEL = Component.literal("[1.5x HARD]");
	private static final Component HARDCORE_LABEL = Component.literal("[2x HARD]");
	private static volatile boolean completed;
	private static volatile boolean hardcoreEscalated;

	private HardModeHudState() {
	}

	public static boolean shouldRender() {
		return completed;
	}

	public static Component label() {
		return hardcoreEscalated ? HARDCORE_LABEL : FIRST_CLEAR_LABEL;
	}

	public static void setCompleted(boolean completed, boolean hardcoreEscalated) {
		HardModeHudState.completed = completed;
		HardModeHudState.hardcoreEscalated = completed && hardcoreEscalated;
	}
}
