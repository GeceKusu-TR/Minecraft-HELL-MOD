package com.example.minecraftmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class SleepRules {
	public static final int MIN_SLEEP_INPUT_CHANGES = 4;
	public static final int MAX_SLEEP_INPUT_CHANGES = 8;
	public static final int SLEEP_TIMEOUT_TICKS = 80;

	private static final Map<ServerPlayer, SleepSession> SESSIONS = Collections.synchronizedMap(new WeakHashMap<>());

	private SleepRules() {
	}

	public static void tick(ServerPlayer player) {
		if (player.level().isClientSide()) {
			return;
		}

		if (!player.isSleeping()) {
			SESSIONS.remove(player);
			return;
		}

		Input currentInput = currentInput(player);
		SleepSession session = SESSIONS.get(player);
		if (session == null) {
			session = new SleepSession(currentInput, requiredChanges(player));
			SESSIONS.put(player, session);
		} else if (!currentInput.equals(session.lastInput)) {
			session.observedChanges++;
			session.lastInput = currentInput;
		}

		if (player.getSleepTimer() >= SLEEP_TIMEOUT_TICKS && session.observedChanges < session.requiredChanges) {
			player.stopSleeping();
			SESSIONS.remove(player);
		}
	}

	private static int requiredChanges(ServerPlayer player) {
		return player.getRandom().nextInt(MAX_SLEEP_INPUT_CHANGES - MIN_SLEEP_INPUT_CHANGES + 1) + MIN_SLEEP_INPUT_CHANGES;
	}

	private static Input currentInput(ServerPlayer player) {
		Input input = player.getLastClientInput();
		return input != null ? input : Input.EMPTY;
	}

	private static final class SleepSession {
		private Input lastInput;
		private final int requiredChanges;
		private int observedChanges;

		private SleepSession(Input initialInput, int requiredChanges) {
			this.lastInput = initialInput;
			this.requiredChanges = requiredChanges;
			this.observedChanges = 0;
		}
	}
}
