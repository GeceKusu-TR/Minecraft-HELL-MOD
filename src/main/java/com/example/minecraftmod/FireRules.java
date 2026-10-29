package com.example.minecraftmod;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.Difficulty;

public final class FireRules {
	public static final int PLAYER_FIRE_DAMAGE_INTERVAL = 20;
	private static final int EASY_FIRE_TICKS = 15 * 20;
	private static final int NORMAL_FIRE_TICKS = 30 * 20;
	private static final int HARD_FIRE_TICKS = 60 * 20;
	private static final float PLAYER_FIRE_DAMAGE = 2.0F;

	private FireRules() {
	}

	public static boolean isFireDamage(DamageSource damageSource) {
		return damageSource.is(DamageTypeTags.IS_FIRE)
			|| damageSource.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
			|| damageSource.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
			|| damageSource.is(net.minecraft.world.damagesource.DamageTypes.LAVA)
			|| damageSource.is(net.minecraft.world.damagesource.DamageTypes.CAMPFIRE)
			|| damageSource.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR);
	}

	public static int fireTicksFor(Level level) {
		return switch (level.getDifficulty()) {
			case EASY -> EASY_FIRE_TICKS;
			case NORMAL -> NORMAL_FIRE_TICKS;
			case HARD -> HARD_FIRE_TICKS;
			case PEACEFUL -> Integer.MAX_VALUE;
		};
	}

	public static boolean shouldBurnForever(Entity entity) {
		return entity.level().getDifficulty() == Difficulty.PEACEFUL || entity.level().getLevelData().isHardcore();
	}

	public static boolean shouldTakePersistentFireDamage(ServerPlayer player) {
		return player.getRemainingFireTicks() > 0 && !player.isInWater() && !player.fireImmune();
	}

	public static void hurtBurningPlayer(ServerLevel level, ServerPlayer player) {
		player.hurtServer(level, player.damageSources().onFire(), PLAYER_FIRE_DAMAGE);
	}
}
