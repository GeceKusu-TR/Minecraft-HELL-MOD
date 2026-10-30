package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

public final class WitherRules {
	private static final double BASE_MAX_HEALTH = 300.0D;
	private static final double MODDED_MAX_HEALTH = 450.0D;
	private static final int VANILLA_ATTACK_INTERVAL = 40;
	private static final int MODDED_ATTACK_INTERVAL = VANILLA_ATTACK_INTERVAL / 2;
	private static final int WITHER_EFFECT_DURATION_TICKS = 20 * 20;
	private static final int WITHER_SHIELD_DAMAGE = 50;
	private static final int PHASE_THREE_ENDERMITE_INTERVAL_TICKS = 20;
	private static final int PHASE_THREE_ENDERMITE_COUNT = 4;
	private static final int PHASE_THREE_ENDERMITE_LIMIT = 50;
	private static final double PHASE_THREE_ENDERMITE_LIMIT_RADIUS = 96.0D;

	private WitherRules() {
	}

	public enum Phase {
		PHASE_1,
		PHASE_2,
		PHASE_3
	}

	public static double maxHealth(double original) {
		if (original == BASE_MAX_HEALTH) {
			return DifficultyScalingRules.more(MODDED_MAX_HEALTH);
		}

		return original;
	}

	public static int attackInterval(int original) {
		if (original == VANILLA_ATTACK_INTERVAL) {
			return DifficultyScalingRules.sooner(MODDED_ATTACK_INTERVAL);
		}

		return original;
	}

	public static Phase phase(WitherBoss wither) {
		float healthRatio = wither.getHealth() / wither.getMaxHealth();
		if (healthRatio <= 0.25F) {
			return Phase.PHASE_3;
		}

		if (healthRatio <= 0.5F) {
			return Phase.PHASE_2;
		}

		return Phase.PHASE_1;
	}

	public static void tickPhaseThree(ServerLevel level, WitherBoss wither) {
		if (phase(wither) != Phase.PHASE_3 || wither.tickCount % DifficultyScalingRules.sooner(PHASE_THREE_ENDERMITE_INTERVAL_TICKS) != 0) {
			return;
		}

		double limitRadius = DifficultyScalingRules.more(PHASE_THREE_ENDERMITE_LIMIT_RADIUS);
		int entityLimit = DifficultyScalingRules.more(PHASE_THREE_ENDERMITE_LIMIT);
		AABB limitArea = wither.getBoundingBox().inflate(limitRadius);
		if (!PerformanceRules.isBelowEntityLimit(level, Endermite.class, limitArea, entityLimit)) {
			return;
		}

		for (int i = 0; i < DifficultyScalingRules.more(PHASE_THREE_ENDERMITE_COUNT); i++) {
			Entity endermite = EntityType.ENDERMITE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (endermite == null) {
				continue;
			}

			Vec3 offset = new Vec3(
				wither.getRandom().nextDouble() * 6.0D - 3.0D,
				0.5D,
				wither.getRandom().nextDouble() * 6.0D - 3.0D
			);
			BlockPos spawnPos = BlockPos.containing(wither.position().add(offset));
			endermite.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
			level.addFreshEntity(endermite);
			if (!PerformanceRules.isBelowEntityLimit(level, Endermite.class, limitArea, entityLimit)) {
				return;
			}
		}
	}

	public static MobEffectInstance extendWitherEffect(MobEffectInstance effect) {
		if (!effect.is(MobEffects.WITHER)) {
			return effect;
		}

		return new MobEffectInstance(
			MobEffects.WITHER,
			 effect.isInfiniteDuration() ? effect.getDuration() : Math.max(effect.getDuration(), DifficultyScalingRules.more(WITHER_EFFECT_DURATION_TICKS)),
			Math.max(effect.getAmplifier(), 2),
			effect.isAmbient(),
			effect.isVisible(),
			effect.showIcon()
		);
	}

	public static void applyExtraWitherEffectDamage(ServerLevel level, LivingEntity entity, int amplifier) {
		float extraDamage = DifficultyScalingRules.more(1.0F + amplifier);
		entity.hurtServer(level, entity.damageSources().wither(), extraDamage);
	}

	public static void handleWitherShieldHit(Player player, net.minecraft.world.damagesource.DamageSource damageSource) {
		if (!isWitherDamage(damageSource) || !player.isBlocking()) {
			return;
		}

		ItemStack shield = player.getUseItem();
		if (shield.isEmpty() || shield.getItem() != Items.SHIELD) {
			return;
		}

		shield.hurtAndBreak(DifficultyScalingRules.more(WITHER_SHIELD_DAMAGE), player, player.getUsedItemHand());
	}

	public static void dropSpeedrunTaxLoot(WitherBoss wither, ServerLevel level) {
		spawnDrop(wither, level, new ItemStack(Items.ENDER_PEARL, 12));
	}

	private static void spawnDrop(WitherBoss wither, ServerLevel level, ItemStack stack) {
		ItemEntity itemEntity = wither.spawnAtLocation(level, stack);
		if (itemEntity != null) {
			itemEntity.setExtendedLifetime();
		}
	}

	private static boolean isWitherDamage(net.minecraft.world.damagesource.DamageSource damageSource) {
		Entity source = damageSource.getEntity();
		Entity directSource = damageSource.getDirectEntity();
		return source instanceof WitherBoss
			|| directSource instanceof WitherBoss
			|| directSource != null && directSource.getType() == EntityType.WITHER_SKULL;
	}
}
