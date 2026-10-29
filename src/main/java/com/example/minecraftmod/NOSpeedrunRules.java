package com.example.minecraftmod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Consumer;

public final class NOSpeedrunRules {
	private NOSpeedrunRules() {
	}

	public static List<ItemStack> removeEnderPearlsFromBarter(List<ItemStack> originalDrops) {
		List<ItemStack> filteredDrops = new ArrayList<>(originalDrops.size());
		for (ItemStack stack : originalDrops) {
			if (!isEnderPearl(stack)) {
				filteredDrops.add(stack);
			}
		}

		return filteredDrops;
	}

	public static Consumer<ItemStack> filterEndermanDeathDrops(Entity source, Consumer<ItemStack> originalDropper) {
		return stack -> {
			if (!isForbiddenEndermanPearlDrop(source, stack)) {
				originalDropper.accept(stack);
			}
		};
	}

	private static boolean isForbiddenEndermanPearlDrop(Entity source, ItemStack stack) {
		return source instanceof EnderMan && isEnderPearl(stack);
	}

	private static boolean isEnderPearl(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL;
	}
}
