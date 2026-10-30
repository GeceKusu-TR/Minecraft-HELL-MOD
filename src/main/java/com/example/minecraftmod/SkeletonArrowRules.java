package com.example.minecraftmod;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.server.level.ServerLevel;

public final class SkeletonArrowRules {
	private static final double SKELETON_ARROW_DAMAGE = 2.0D;
	private static final int SHIELD_DAMAGE = 20;

	private SkeletonArrowRules() {
	}

	public static boolean handleSkeletonArrowHit(AbstractArrow arrow, EntityHitResult hitResult) {
		if (!(arrow.getOwner() instanceof AbstractSkeleton)) {
			return false;
		}

		Entity hitEntity = hitResult.getEntity();
		if (!(hitEntity instanceof Player player)) {
			arrow.setBaseDamage(DifficultyScalingRules.more(SKELETON_ARROW_DAMAGE));
			return false;
		}

		if (isBlockingWithShield(player)) {
			damageActiveShield(player);
			arrow.discard();
			return true;
		}

		arrow.setBaseDamage(DifficultyScalingRules.more(SKELETON_ARROW_DAMAGE));
		return false;
	}

	public static boolean hurtWithSkeletonArrowCap(AbstractArrow arrow, Entity hitEntity, DamageSource damageSource, float amount) {
		if (arrow.getOwner() instanceof AbstractSkeleton && hitEntity instanceof Player) {
			return hurtOnServer(hitEntity, damageSource, (float) Math.min(amount, DifficultyScalingRules.more(SKELETON_ARROW_DAMAGE)));
		}

		return hurtOnServer(hitEntity, damageSource, amount);
	}

	private static boolean isBlockingWithShield(Player player) {
		ItemStack useItem = player.getUseItem();
		return player.isBlocking() && !useItem.isEmpty() && useItem.getItem() == Items.SHIELD;
	}

	private static void damageActiveShield(Player player) {
		InteractionHand hand = player.getUsedItemHand();
		ItemStack shield = player.getUseItem();
		if (shield.isEmpty()) {
			return;
		}

		shield.hurtAndBreak(DifficultyScalingRules.more(SHIELD_DAMAGE), player, hand);
	}

	private static boolean hurtOnServer(Entity entity, DamageSource damageSource, float amount) {
		if (entity.level() instanceof ServerLevel level) {
			return entity.hurtServer(level, damageSource, amount);
		}
		return false;
	}
}
