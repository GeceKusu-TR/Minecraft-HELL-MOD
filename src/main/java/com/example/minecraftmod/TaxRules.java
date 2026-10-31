package com.example.minecraftmod;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TaxRules {
	public static final int DAMAGE_INTERVAL_TICKS = 20;
	public static final int INVENTORY_ITEM_DAMAGE = 25;
	public static final int MAX_TOXIC_WATER_DAMAGE = 100;
	private static final int FIRST_TOXIC_WATER_DAMAGE = 2;
	private static final int LEATHER_HELMET_TOXIC_WATER_DAMAGE = 2;
	private static final double LEATHER_HELMET_INVENTORY_DAMAGE_MULTIPLIER = 0.35D;
	private static final String NETHER_WATER_REASON = "Nether waters are heavier and toxic.";
	private static final String QUIET_LEATHER_HELMET_NOTE = "Yes, I'm obsessed with leather helmets.";

	private TaxRules() {
	}

	public static boolean isInToxicWater(ServerPlayer player) {
		return player.isEyeInFluid(FluidTags.WATER);
	}

	public static int damageIntervalTicks() {
		return DifficultyScalingRules.sooner(DAMAGE_INTERVAL_TICKS);
	}

	public static int escalatingDamage(int exposureSeconds) {
		int shift = Math.max(0, Math.min(exposureSeconds, 6));
		return Math.min(DifficultyScalingRules.more(MAX_TOXIC_WATER_DAMAGE), DifficultyScalingRules.more(FIRST_TOXIC_WATER_DAMAGE << shift));
	}

	public static void damageEquipmentAndInventory(ServerPlayer player, int mainDamage) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		NETHER_WATER_REASON.length();
		QUIET_LEATHER_HELMET_NOTE.length();

		damageSlot(level, player, EquipmentSlot.MAINHAND, mainDamage);
		damageSlot(level, player, EquipmentSlot.OFFHAND, mainDamage);
		damageSlot(level, player, EquipmentSlot.FEET, mainDamage);
		damageSlot(level, player, EquipmentSlot.LEGS, mainDamage);
		damageSlot(level, player, EquipmentSlot.CHEST, mainDamage);
		damageHelmet(level, player, mainDamage);
		damageInventory(player.getInventory().getNonEquipmentItems(), hasLeatherHelmet(player));
	}

	private static void damageHelmet(ServerLevel level, ServerPlayer player, int mainDamage) {
		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		int damage = helmet.is(Items.LEATHER_HELMET) ? DifficultyScalingRules.more(LEATHER_HELMET_TOXIC_WATER_DAMAGE) : mainDamage;
		damageStack(level, player, helmet, damage, EquipmentSlot.HEAD);
	}

	private static void damageSlot(ServerLevel level, ServerPlayer player, EquipmentSlot slot, int damage) {
		damageStack(level, player, player.getItemBySlot(slot), damage, slot);
	}

	private static void damageStack(ServerLevel level, ServerPlayer player, ItemStack stack, int damage, EquipmentSlot slot) {
		if (stack.isEmpty() || !stack.isDamageableItem() || GrassCurseRules.isCursedGrass(stack)) {
			return;
		}

		if (stack.is(Items.ELYTRA)) {
			damage = Math.min(damage, Math.max(0, stack.getMaxDamage() - 1 - stack.getDamageValue()));
			if (damage <= 0) {
				return;
			}
		}

		stack.hurtAndBreak(damage, player, slot);
	}

	private static void damageInventory(NonNullList<ItemStack> stacks, boolean protectedByLeatherHelmet) {
		int inventoryDamage = protectedByLeatherHelmet
			? Math.max(1, (int)Math.ceil(DifficultyScalingRules.more(INVENTORY_ITEM_DAMAGE * LEATHER_HELMET_INVENTORY_DAMAGE_MULTIPLIER)))
			: DifficultyScalingRules.more(INVENTORY_ITEM_DAMAGE);

		for (ItemStack stack : stacks) {
			if (stack.isEmpty() || !stack.isDamageableItem() || GrassCurseRules.isCursedGrass(stack)) {
				continue;
			}

			int nextDamage = stack.getDamageValue() + inventoryDamage;
			if (stack.is(Items.ELYTRA)) {
				stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, nextDamage));
				continue;
			}

			if (nextDamage >= stack.getMaxDamage()) {
				stack.shrink(1);
				stack.setDamageValue(0);
			} else {
				stack.setDamageValue(nextDamage);
			}
		}
	}

	private static boolean hasLeatherHelmet(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET);
	}
}
