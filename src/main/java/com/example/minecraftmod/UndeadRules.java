package com.example.minecraftmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class UndeadRules {
	private UndeadRules() {
	}

	public static boolean isProtectedUndead(Entity entity) {
		return entity instanceof Zombie || entity instanceof AbstractSkeleton;
	}

	public static void ensureLeatherHelmet(LivingEntity livingEntity) {
		ItemStack helmet = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
		if (!helmet.is(Items.LEATHER_HELMET)) {
			livingEntity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		}
	}
}
