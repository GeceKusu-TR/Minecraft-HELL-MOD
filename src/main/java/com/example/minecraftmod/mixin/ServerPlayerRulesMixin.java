package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AdvancementRules;
import com.example.minecraftmod.CompletionHudPayload;
import com.example.minecraftmod.DifficultyScalingRules;
import com.example.minecraftmod.EndCompletionRules;
import com.example.minecraftmod.EndExperienceRules;
import com.example.minecraftmod.EndTermsRules;
import com.example.minecraftmod.FakePlayerRules;
import com.example.minecraftmod.FireRules;
import com.example.minecraftmod.FishingRules;
import com.example.minecraftmod.GrassCurseRules;
import com.example.minecraftmod.LeatherHelmetRules;
import com.example.minecraftmod.PlayerRules;
import com.example.minecraftmod.NetherRules;
import com.example.minecraftmod.TaxRules;
import com.example.minecraftmod.TestBookRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRulesMixin {
	@Unique
	private boolean minecraftmod$completionHudSyncInitialized;

	@Unique
	private boolean minecraftmod$completionHudCompleted;

	@Unique
	private int minecraftmod$sprintLockTicks;

	@Unique
	private int minecraftmod$sprintTicks;

	@Unique
	private int minecraftmod$postSprintSlowdownTicks;

	@Unique
	private boolean minecraftmod$wasSprinting;

	@Unique
	private int minecraftmod$toxicWaterTicks;

	@Unique
	private int minecraftmod$toxicWaterSeconds;

	@Unique
	private int minecraftmod$airFallDamageStep;

	@Unique
	private int minecraftmod$fallAccelerationStep;

	@Unique
	private boolean minecraftmod$wasOnGround = true;

	@Unique
	private boolean minecraftmod$suppressCurrentAirborneFall;

	@Inject(method = "tick", at = @At("TAIL"))
	private void minecraftmod$tickPlayerRules(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		minecraftmod$syncCompletionHud(player);
		DifficultyScalingRules.restoreFromPlayer(player);
		if (!player.isAlive() || player.isDeadOrDying()) {
			minecraftmod$resetFallState(player);
			return;
		}

		if (TestBookRules.tick(player)) {
			minecraftmod$resetFallState(player);
			PlayerRules.clearWalkSlowdown(player);
			return;
		}

		minecraftmod$tickAirborneFallState(player);
		minecraftmod$tickSprintPenalty(player);
		minecraftmod$tickToxicNetherWater(player);
		minecraftmod$tickPersistentFireDamage(player);
		AdvancementRules.clearGhastFireballBurnIfNotBurning(player);
		GrassCurseRules.tickPlayer(player);
		FakePlayerRules.tick(player);
		EndExperienceRules.tick(player);
		FishingRules.curseInventoryFishingRods(player);
		LeatherHelmetRules.tickWornHelmet(player);
		AdvancementRules.grantWelcomeToHell(player);
		AdvancementRules.grantWelcomeToOverworldIfInNether(player);
		AdvancementRules.tickInventoryAdvancements(player);
		minecraftmod$fallAccelerationStep = PlayerRules.accelerateDangerousFalls(player, minecraftmod$fallAccelerationStep, minecraftmod$suppressCurrentAirborneFall);
		minecraftmod$airFallDamageStep = PlayerRules.tickAirFallDamage(player, minecraftmod$airFallDamageStep, minecraftmod$suppressCurrentAirborneFall);
	}

	@Unique
	private void minecraftmod$syncCompletionHud(ServerPlayer player) {
		boolean completed = EndCompletionRules.isCompleted(player);
		boolean hardcoreEscalated = EndCompletionRules.isHardcoreEscalated(player);
		if (!minecraftmod$completionHudSyncInitialized
			|| minecraftmod$completionHudCompleted != completed
			|| player.tickCount % 20 == 0) {
			if (ServerPlayNetworking.canSend(player, CompletionHudPayload.TYPE)) {
				ServerPlayNetworking.send(player, new CompletionHudPayload(completed, hardcoreEscalated));
			}
			minecraftmod$completionHudSyncInitialized = true;
			minecraftmod$completionHudCompleted = completed;
		}
	}

	@Inject(method = "die", at = @At("HEAD"))
	private void minecraftmod$resetFallStateOnDeath(DamageSource damageSource, CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		AdvancementRules.grantGhastDeath(player, damageSource);
		EndExperienceRules.clearPlayer(player);
		EndTermsRules.onDisconnect(player);
		minecraftmod$resetFallState(player);
	}

	@Unique
	private void minecraftmod$resetFallState(ServerPlayer player) {
		minecraftmod$wasOnGround = true;
		minecraftmod$suppressCurrentAirborneFall = false;
		minecraftmod$airFallDamageStep = 0;
		minecraftmod$fallAccelerationStep = 0;
		player.fallDistance = 0.0F;
		Vec3 movement = player.getDeltaMovement();
		if (movement.y < 0.0D) {
			player.setDeltaMovement(movement.x, 0.0D, movement.z);
			player.hurtMarked = true;
		}
	}

	@Unique
	private void minecraftmod$tickAirborneFallState(ServerPlayer player) {
		if (player.onGround()) {
			minecraftmod$resetFallState(player);
			return;
		}

		if (minecraftmod$wasOnGround) {
			minecraftmod$suppressCurrentAirborneFall = player.getDeltaMovement().y > 0.0D;
		}

		minecraftmod$wasOnGround = false;
	}

	@Unique
	private void minecraftmod$tickSprintPenalty(ServerPlayer player) {
		boolean overworldRestriction = player.level().dimension() == Level.OVERWORLD;
		boolean hardcoreRestriction = player.level().getLevelData().isHardcore();
		if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL
			|| NetherRules.disablesSprintSlowdown(player)
			|| !overworldRestriction && !hardcoreRestriction) {
			minecraftmod$wasSprinting = false;
			minecraftmod$sprintTicks = 0;
			minecraftmod$sprintLockTicks = 0;
			minecraftmod$postSprintSlowdownTicks = 0;
			PlayerRules.clearWalkSlowdown(player);
			return;
		}

		if (minecraftmod$sprintLockTicks > 0) {
			minecraftmod$sprintTicks = 0;
			if (player.isSprinting()) {
				player.setSprinting(false);
			}

			minecraftmod$sprintLockTicks--;
			if (minecraftmod$postSprintSlowdownTicks > 0) {
				PlayerRules.applyWalkSlowdown(player);
				minecraftmod$postSprintSlowdownTicks--;
			} else {
				PlayerRules.clearWalkSlowdown(player);
			}
			return;
		}

		if (player.isSprinting()) {
			PlayerRules.clearWalkSlowdown(player);
			minecraftmod$wasSprinting = true;
			minecraftmod$sprintTicks++;
			if (minecraftmod$sprintTicks >= PlayerRules.SPRINT_PENALTY_DELAY_TICKS) {
				minecraftmod$sprintTicks = 0;
				minecraftmod$sprintLockTicks = PlayerRules.SPRINT_LOCK_TICKS;
				minecraftmod$postSprintSlowdownTicks = PlayerRules.WALK_SLOWDOWN_TICKS;
				PlayerRules.stopSprintAndSlowWalking(player);
			}
			return;
		}

		if (minecraftmod$wasSprinting) {
			minecraftmod$wasSprinting = false;
		}

		minecraftmod$sprintTicks = 0;
		PlayerRules.clearWalkSlowdown(player);
	}

	@Unique
	private void minecraftmod$tickToxicNetherWater(ServerPlayer player) {
		if (!TaxRules.isInToxicNetherWater(player)) {
			minecraftmod$toxicWaterTicks = 0;
			minecraftmod$toxicWaterSeconds = 0;
			return;
		}

		minecraftmod$toxicWaterTicks++;
		if (minecraftmod$toxicWaterTicks < TaxRules.DAMAGE_INTERVAL_TICKS) {
			return;
		}

		minecraftmod$toxicWaterTicks = 0;
		int damage = TaxRules.escalatingDamage(minecraftmod$toxicWaterSeconds);
		TaxRules.damageEquipmentAndInventory(player, damage);
		minecraftmod$toxicWaterSeconds++;
	}

	@Unique
	private void minecraftmod$tickPersistentFireDamage(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || player.tickCount % FireRules.playerFireDamageInterval() != 0) {
			return;
		}

		if (FireRules.shouldTakePersistentFireDamage(player)) {
			FireRules.hurtBurningPlayer(level, player);
		}
	}
}
