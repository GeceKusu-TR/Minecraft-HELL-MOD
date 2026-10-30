package com.example.minecraftmod;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class PlayerRules {
	public static final int SPRINT_PENALTY_DELAY_TICKS = 60;
	public static final int SPRINT_LOCK_TICKS = 40;
	public static final int WALK_SLOWDOWN_TICKS = SPRINT_LOCK_TICKS;
	public static final Identifier POST_SPRINT_SLOWDOWN_ID = MinecraftMod.id("post_sprint_slowdown");
	private static final double WALK_SLOWDOWN_MULTIPLIER = -0.5D;
	private static final float FALL_ACCELERATION_STEP_BLOCKS = 5.0F;
	private static final double FALL_ACCELERATION_BASE_SPEED = 0.25D;
	private static final double FALL_ACCELERATION_SPEED_PER_STEP = 1.0D;
	private static final double FALL_ACCELERATION_MAX_DOWNWARD_SPEED = 6.0D;
	// Helmet 50% fall protection will not be added haaahah.
	private static final float JUMP_STARTED_FALL_GRACE_BLOCKS = 3.0F;
	private static final float AIR_FALL_DAMAGE_STEP_BLOCKS = 1.0F;
	private static final float AIR_FALL_DAMAGE_PER_STEP = 1.0F;

	private PlayerRules() {
	}

	public static void stopSprintAndSlowWalking(ServerPlayer player) {
		if (player.isSprinting()) {
			player.setSprinting(false);
		}

		applyWalkSlowdown(player);
	}

	public static void applyWalkSlowdown(ServerPlayer player) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed == null || movementSpeed.getModifier(POST_SPRINT_SLOWDOWN_ID) != null) {
			return;
		}

		movementSpeed.addTransientModifier(new AttributeModifier(
			POST_SPRINT_SLOWDOWN_ID,
			WALK_SLOWDOWN_MULTIPLIER,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		));
	}

	public static void clearWalkSlowdown(ServerPlayer player) {
		AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movementSpeed != null) {
			movementSpeed.removeModifier(POST_SPRINT_SLOWDOWN_ID);
		}
	}

	public static int accelerateDangerousFalls(ServerPlayer player, int previousAccelerationStep, boolean suppressCurrentAirborneFall) {
		if (!canApplyDangerousFallRules(player) || isJumpGraceActive(player, suppressCurrentAirborneFall)) {
			return 0;
		}

		Vec3 movement = player.getDeltaMovement();
		if (player.onGround() || movement.y >= -0.08D || player.fallDistance < FALL_ACCELERATION_STEP_BLOCKS) {
			return 0;
		}

		int fallSteps = (int)(player.fallDistance / FALL_ACCELERATION_STEP_BLOCKS);
		if (fallSteps <= previousAccelerationStep) {
			return Math.max(previousAccelerationStep, fallSteps);
		}

		double targetDownwardSpeed = Math.min(
			FALL_ACCELERATION_BASE_SPEED + fallSteps * FALL_ACCELERATION_SPEED_PER_STEP,
			FALL_ACCELERATION_MAX_DOWNWARD_SPEED
		);
		double nextY = Math.min(movement.y, -targetDownwardSpeed);
		if (nextY >= movement.y) {
			return fallSteps;
		}

		player.setDeltaMovement(movement.x, nextY, movement.z);
		player.hurtMarked = true;
		return fallSteps;
	}

	public static int tickAirFallDamage(ServerPlayer player, int previousDamageStep, boolean suppressCurrentAirborneFall) {
		if (!canApplyDangerousFallRules(player)) {
			return 0;
		}

		if (isJumpGraceActive(player, suppressCurrentAirborneFall)) {
			int currentDamageStep = (int)(player.fallDistance / AIR_FALL_DAMAGE_STEP_BLOCKS);
			return Math.max(previousDamageStep, currentDamageStep);
		}

		Vec3 movement = player.getDeltaMovement();
		if (player.onGround() || movement.y >= -0.08D || player.fallDistance < AIR_FALL_DAMAGE_STEP_BLOCKS) {
			return 0;
		}

		int currentDamageStep = (int)(player.fallDistance / AIR_FALL_DAMAGE_STEP_BLOCKS);
		if (currentDamageStep <= previousDamageStep || !(player.level() instanceof ServerLevel level)) {
			return Math.max(previousDamageStep, currentDamageStep);
		}

		player.hurtServer(level, player.damageSources().fall(), DifficultyScalingRules.more(AIR_FALL_DAMAGE_PER_STEP));
		return previousDamageStep + 1;
	}

	private static boolean isJumpGraceActive(ServerPlayer player, boolean suppressCurrentAirborneFall) {
		return suppressCurrentAirborneFall && player.fallDistance < JUMP_STARTED_FALL_GRACE_BLOCKS;
	}

	private static boolean canApplyDangerousFallRules(ServerPlayer player) {
		return player.isAlive()
			&& !player.isDeadOrDying()
			&& player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
			&& player.level().getDifficulty() != Difficulty.PEACEFUL
			&& !player.isFallFlying()
			&& !player.isInWater()
			&& !player.isInLava()
			&& !player.onClimbable()
			&& !player.hasEffect(MobEffects.SLOW_FALLING)
			&& !player.hasEffect(MobEffects.LEVITATION);
	}
}
