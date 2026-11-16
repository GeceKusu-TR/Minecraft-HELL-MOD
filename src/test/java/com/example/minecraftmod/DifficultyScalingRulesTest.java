package com.example.minecraftmod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DifficultyScalingRulesTest {
	@AfterEach
	void restoreBaseDifficulty() {
		DifficultyScalingRules.reset();
	}

	@Test
	void scalesDamageAndIntervalsForEachEscalation() {
		assertEquals(2, DifficultyScalingRules.more(2));
		assertEquals(20, DifficultyScalingRules.sooner(20));

		DifficultyScalingRules.activateNormalEscalation();
		assertEquals(3, DifficultyScalingRules.more(2));
		assertEquals(13, DifficultyScalingRules.sooner(20));

		DifficultyScalingRules.activateHardcoreEscalation();
		assertEquals(4, DifficultyScalingRules.more(2));
		assertEquals(10, DifficultyScalingRules.sooner(20));
	}

	@Test
	void resetDoesNotLeakDifficultyBetweenServers() {
		DifficultyScalingRules.activateHardcoreEscalation();

		DifficultyScalingRules.reset();

		assertEquals(1.0D, DifficultyScalingRules.multiplier());
		assertEquals(20, PerformanceRules.endEndermanLimit());
	}

	@Test
	void anUncompletedPlayerCannotDowngradeAnEscalatedServer() {
		DifficultyScalingRules.activateNormalEscalation();

		DifficultyScalingRules.restoreFromCompletion(false, false);

		assertEquals(1.5D, DifficultyScalingRules.multiplier());
	}

	@Test
	void endermanLimitUsesTheSameEscalationAsOtherRules() {
		DifficultyScalingRules.activateNormalEscalation();
		assertEquals(30, PerformanceRules.endEndermanLimit());

		DifficultyScalingRules.activateHardcoreEscalation();
		assertEquals(40, PerformanceRules.endEndermanLimit());
	}
}
