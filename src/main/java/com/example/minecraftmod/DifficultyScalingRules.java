package com.example.minecraftmod;

import net.minecraft.server.level.ServerPlayer;

/** Central multiplier for the mod's added difficulty systems. */
public final class DifficultyScalingRules {
	public static final String COMPLETED_TAG = "minecraftmod_end_completed";
	public static final String HARDCORE_ESCALATED_TAG = "minecraftmod_hardcore_2x";
	private static final double BASE_MULTIPLIER = 1.0D;
	private static final double FIRST_CLEAR_MULTIPLIER = 1.5D;
	private static final double HARDCORE_CLEAR_MULTIPLIER = 2.0D;
	private static volatile double activeMultiplier = BASE_MULTIPLIER;

	private DifficultyScalingRules() {
	}

	public static void activateNormalEscalation() {
		activeMultiplier = FIRST_CLEAR_MULTIPLIER;
	}

	public static void activateHardcoreEscalation() {
		activeMultiplier = HARDCORE_CLEAR_MULTIPLIER;
	}

	public static void restoreFromPlayer(ServerPlayer player) {
		boolean completed = player.getTags().contains(COMPLETED_TAG);
		if (player.level().getLevelData().isHardcore() && completed) {
			activateHardcoreEscalation();
		} else if (!player.level().getLevelData().isHardcore() && completed) {
			activateNormalEscalation();
		} else {
			activeMultiplier = BASE_MULTIPLIER;
		}
	}

	public static double multiplier() {
		return activeMultiplier;
	}

	public static int more(int base) {
		return Math.max(1, (int)Math.ceil(base * activeMultiplier));
	}

	public static float more(float base) {
		return (float)(base * activeMultiplier);
	}

	public static double more(double base) {
		return base * activeMultiplier;
	}

	public static int sooner(int base) {
		return Math.max(1, (int)Math.floor(base / activeMultiplier));
	}
}
