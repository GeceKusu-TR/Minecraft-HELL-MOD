package com.example.minecraftmod;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class StoneRules {
	private static final Set<Block> STONE_BLOCKS = Set.of(
		Blocks.STONE,
		Blocks.COBBLESTONE,
		Blocks.SMOOTH_STONE,
		Blocks.GRANITE,
		Blocks.POLISHED_GRANITE,
		Blocks.DIORITE,
		Blocks.POLISHED_DIORITE,
		Blocks.ANDESITE,
		Blocks.POLISHED_ANDESITE,
		Blocks.TUFF,
		Blocks.POLISHED_TUFF,
		Blocks.TUFF_BRICKS,
		Blocks.CHISELED_TUFF,
		Blocks.CHISELED_TUFF_BRICKS,
		Blocks.STONE_BRICKS,
		Blocks.MOSSY_STONE_BRICKS,
		Blocks.CRACKED_STONE_BRICKS,
		Blocks.CHISELED_STONE_BRICKS,
		Blocks.INFESTED_STONE,
		Blocks.INFESTED_STONE_BRICKS,
		Blocks.INFESTED_MOSSY_STONE_BRICKS,
		Blocks.INFESTED_CRACKED_STONE_BRICKS,
		Blocks.INFESTED_CHISELED_STONE_BRICKS
	);

	private static final Set<Block> ORE_BLOCKS = Set.of(
		Blocks.COAL_ORE,
		Blocks.IRON_ORE,
		Blocks.COPPER_ORE,
		Blocks.GOLD_ORE,
		Blocks.REDSTONE_ORE,
		Blocks.LAPIS_ORE,
		Blocks.DIAMOND_ORE,
		Blocks.EMERALD_ORE,
		Blocks.NETHER_GOLD_ORE,
		Blocks.NETHER_QUARTZ_ORE,
		Blocks.ANCIENT_DEBRIS
	);

	private static final Set<Block> DEEPSLATE_ORE_BLOCKS = Set.of(
		Blocks.DEEPSLATE_COAL_ORE,
		Blocks.DEEPSLATE_COPPER_ORE,
		Blocks.DEEPSLATE_IRON_ORE,
		Blocks.DEEPSLATE_GOLD_ORE,
		Blocks.DEEPSLATE_REDSTONE_ORE,
		Blocks.DEEPSLATE_LAPIS_ORE,
		Blocks.DEEPSLATE_DIAMOND_ORE,
		Blocks.DEEPSLATE_EMERALD_ORE
	);

	private static final Set<Block> DEEPSLATE_BLOCKS = Set.of(
		Blocks.DEEPSLATE,
		Blocks.COBBLED_DEEPSLATE,
		Blocks.POLISHED_DEEPSLATE,
		Blocks.DEEPSLATE_BRICKS,
		Blocks.CHISELED_DEEPSLATE,
		Blocks.CRACKED_DEEPSLATE_BRICKS,
		Blocks.DEEPSLATE_TILES,
		Blocks.CRACKED_DEEPSLATE_TILES,
		Blocks.INFESTED_DEEPSLATE,
		Blocks.REINFORCED_DEEPSLATE
	);

	private StoneRules() {
	}

	public static boolean isPickaxe(ItemStack itemStack) {
		return itemStack.is(ItemTags.PICKAXES);
	}

	public static boolean isStoneLikeBlock(BlockState blockState) {
		return isStoneFamily(blockState);
	}

	public static int getAdditionalDurabilityDamage(BlockState blockState, ItemStack itemStack) {
		if (!isPickaxe(itemStack)) {
			return 0;
		}

		if (isDeepslateFamily(blockState)) {
			if (isNetheritePickaxe(itemStack)) {
				return 0;
			}

			if (isDiamondPickaxe(itemStack)) {
				return 1;
			}

			return itemStack.getMaxDamage();
		}

		if (isStoneFamily(blockState)) {
			return 1;
		}

		return 0;
	}

	public static boolean isOreBlock(BlockState blockState) {
		return ORE_BLOCKS.contains(blockState.getBlock()) || DEEPSLATE_ORE_BLOCKS.contains(blockState.getBlock());
	}

	private static boolean isStoneFamily(BlockState blockState) {
		return blockState.is(BlockTags.BASE_STONE_OVERWORLD)
			|| STONE_BLOCKS.contains(blockState.getBlock())
			|| ORE_BLOCKS.contains(blockState.getBlock())
			|| DEEPSLATE_ORE_BLOCKS.contains(blockState.getBlock());
	}

	private static boolean isDeepslateFamily(BlockState blockState) {
		return DEEPSLATE_BLOCKS.contains(blockState.getBlock())
			|| DEEPSLATE_ORE_BLOCKS.contains(blockState.getBlock());
	}

	private static boolean isDiamondPickaxe(ItemStack itemStack) {
		return itemStack.is(Items.DIAMOND_PICKAXE) || itemStack.is(Items.NETHERITE_PICKAXE);
	}

	private static boolean isNetheritePickaxe(ItemStack itemStack) {
		return itemStack.is(Items.NETHERITE_PICKAXE);
	}
}
