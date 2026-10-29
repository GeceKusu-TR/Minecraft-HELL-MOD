package com.example.minecraftmod;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class LeatherHelmetRules {
	public static final int MAX_DAMAGE = 20;
	public static final int PASSIVE_DAMAGE_INTERVAL_TICKS = 20;

	private LeatherHelmetRules() {
	}

	public static boolean isLeatherHelmet(ItemStack stack) {
		return stack.is(Items.LEATHER_HELMET);
	}

	public static boolean isLeatherArmor(ItemStack stack) {
		return stack.is(Items.LEATHER_HELMET)
			|| stack.is(Items.LEATHER_CHESTPLATE)
			|| stack.is(Items.LEATHER_LEGGINGS)
			|| stack.is(Items.LEATHER_BOOTS);
	}

	public static boolean hasFullLeatherArmorSet(ServerPlayer player) {
		return hasFullLeatherArmorSet((net.minecraft.world.entity.player.Player)player);
	}

	public static boolean hasFullLeatherArmorSet(net.minecraft.world.entity.player.Player player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET)
			&& player.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE)
			&& player.getItemBySlot(EquipmentSlot.LEGS).is(Items.LEATHER_LEGGINGS)
			&& player.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS);
	}

	public static void tickWornHelmet(ServerPlayer player) {
		for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
			ItemStack armor = player.getItemBySlot(slot);
			if (!isLeatherArmor(armor)) {
				continue;
			}

			removeForbiddenEnchantments(armor, player.level().registryAccess());
			if (player.tickCount % PASSIVE_DAMAGE_INTERVAL_TICKS == 0 && player.level() instanceof ServerLevel level) {
				armor.hurtAndBreak(1, level, player, item -> player.onEquippedItemBroken(item, slot));
			}
		}
	}

	public static boolean hasForbiddenEnchantments(ItemStack stack, HolderLookup.Provider holders) {
		if (!isLeatherArmor(stack)) {
			return false;
		}

		var enchantments = holders.lookupOrThrow(Registries.ENCHANTMENT);
		return EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.UNBREAKING), stack) > 0
			|| EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.MENDING), stack) > 0;
	}

	public static void removeForbiddenEnchantments(ItemStack stack, HolderLookup.Provider holders) {
		if (!isLeatherArmor(stack)) {
			return;
		}

		Holder<Enchantment> unbreaking = holders.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
		Holder<Enchantment> mending = holders.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(enchantment -> enchantment.equals(unbreaking) || enchantment.equals(mending)));
	}
}
