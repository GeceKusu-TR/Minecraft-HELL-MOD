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

	/**
	 * Resets the process-local multiplier when a new Minecraft server starts.
	 *
	 * <p>The actual completion state is stored on players, so it is restored on
	 * join. Keeping this reset at the server boundary prevents an integrated
	 * server from inheriting the previous world's difficulty.</p>
	 */
	public static void reset() {
		activeMultiplier = BASE_MULTIPLIER;
	}

	public static void activateNormalEscalation() {
		activeMultiplier = FIRST_CLEAR_MULTIPLIER;
	}

	public static void activateHardcoreEscalation() {
		activeMultiplier = HARDCORE_CLEAR_MULTIPLIER;
	}

	public static void restoreFromPlayer(ServerPlayer player) {
		restoreFromCompletion(player.level().getLevelData().isHardcore(), player.getTags().contains(COMPLETED_TAG));
	}

	static void restoreFromCompletion(boolean hardcore, boolean completed) {
		if (!completed) {
			return;
		}

		if (hardcore) {
			activateHardcoreEscalation();
		} else {
			activateNormalEscalation();
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
