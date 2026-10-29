package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OxygenRules;
import com.example.minecraftmod.OxygenEffects;
import com.example.minecraftmod.OxygenDamageTypes;
import com.example.minecraftmod.TestBookRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerOxygenMixin {
	@Unique
	private int minecraftmod$oxygenDamageTicks;

	@Unique
	private OxygenRules.OxygenStage minecraftmod$currentOxygenStage = OxygenRules.OxygenStage.NONE;

	@Unique
	private boolean minecraftmod$wasBreathingButtonPressed;

	@Inject(method = "die", at = @At("HEAD"))
	private void minecraftmod$clearOxygenTitlesOnDeath(DamageSource damageSource, CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		minecraftmod$clearOxygenState(player);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void minecraftmod$tickOxygenShortage(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		if (player.level().isClientSide() || !player.isAlive() || player.isDeadOrDying()) {
			return;
		}
		if (TestBookRules.isReadingRequiredBooks(player)) {
			minecraftmod$clearOxygenState(player);
			return;
		}

		minecraftmod$tickAltitudeOxygen(player);
		minecraftmod$tickUnderwaterBreathing(player);
	}

	@Unique
	private void minecraftmod$tickAltitudeOxygen(ServerPlayer player) {
		OxygenRules.OxygenStage oxygenStage = OxygenRules.oxygenStage(player);
		OxygenRules.OxygenStage previousStage = minecraftmod$currentOxygenStage;
		if (oxygenStage != OxygenRules.OxygenStage.NONE) {
			if (previousStage == OxygenRules.OxygenStage.NONE || !player.hasEffect(OxygenEffects.OXYGENLESS)) {
				OxygenRules.applyInfiniteEffect(player);
			}

			if (previousStage != oxygenStage) {
				minecraftmod$oxygenDamageTicks = 0;
				OxygenRules.showTitle(player, OxygenRules.titleForStage(oxygenStage));
			}

			minecraftmod$currentOxygenStage = oxygenStage;
			minecraftmod$oxygenDamageTicks++;

			if (minecraftmod$oxygenDamageTicks >= OxygenRules.DAMAGE_INTERVAL) {
				float damage = OxygenRules.oxygenDeprivationDamage(player);
				if (damage > 0.0F) {
					player.hurtServer((ServerLevel)player.level(), OxygenDamageTypes.oxygenDeprivation(player.level()), damage);
				}
				minecraftmod$oxygenDamageTicks = 0;
			}
			return;
		}

		if (previousStage != OxygenRules.OxygenStage.NONE || player.hasEffect(OxygenEffects.OXYGENLESS)) {
			minecraftmod$clearAltitudeOxygenState(player);
		}
	}

	@Unique
	private void minecraftmod$clearOxygenState(ServerPlayer player) {
		minecraftmod$clearAltitudeOxygenState(player);
		OxygenRules.clearPeacefulHealthCap(player);
		OxygenRules.clearTurtleShellHealthBoost(player);
	}

	@Unique
	private void minecraftmod$clearAltitudeOxygenState(ServerPlayer player) {
		minecraftmod$currentOxygenStage = OxygenRules.OxygenStage.NONE;
		minecraftmod$oxygenDamageTicks = 0;
		OxygenRules.clearTitle(player);
		player.removeEffect(OxygenEffects.OXYGENLESS);
	}

	@Unique
	private void minecraftmod$tickUnderwaterBreathing(ServerPlayer player) {
		boolean breathingButtonPressed = OxygenRules.isBreathingButtonPressed(player);
		boolean breathingButtonJustPressed = breathingButtonPressed && !minecraftmod$wasBreathingButtonPressed;
		minecraftmod$wasBreathingButtonPressed = breathingButtonPressed;

		if (OxygenRules.isPeaceful(player)) {
			OxygenRules.clearTurtleShellHealthBoost(player);
			OxygenRules.applyPeacefulHealthCap(player);
			OxygenRules.applyPeacefulFoodCap(player);
			return;
		}

		OxygenRules.clearPeacefulHealthCap(player);

		if (!OxygenRules.isUnderwater(player)) {
			OxygenRules.clearTurtleShellHealthBoost(player);
			return;
		}

		if (OxygenRules.hasLeatherHelmet(player)) {
			OxygenRules.clearTurtleShellHealthBoost(player);
			return;
		}

		if (OxygenRules.hasTurtleShellHelmet(player)) {
			OxygenRules.applyTurtleShellHealthBoost(player);
		} else {
			OxygenRules.clearTurtleShellHealthBoost(player);
		}

		if (breathingButtonJustPressed) {
			player.setAirSupply(20);
			return;
		}

		if (!OxygenRules.isOutOfAir(player)) {
			return;
		}

		if (player.tickCount % OxygenRules.DAMAGE_INTERVAL == 0) {
			player.hurtServer((ServerLevel)player.level(), player.damageSources().drown(), 1.0F);
		}
	}
}
