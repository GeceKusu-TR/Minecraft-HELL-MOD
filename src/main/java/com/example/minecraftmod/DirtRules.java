package com.example.minecraftmod;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class DirtRules {
	private DirtRules() {
	}

	public static boolean isDirt(BlockState blockState) {
		return blockState.is(Blocks.DIRT) || blockState.is(Blocks.GRASS_BLOCK) || blockState.is(BlockTags.DIRT);
	}

	public static boolean isShovel(ItemStack itemStack) {
		return itemStack.is(ItemTags.SHOVELS);
	}
}
