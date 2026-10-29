package com.example.minecraftmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PiglinRules {
	private static final float ROOTED_DIRT_BARTER_CHANCE = 0.75F;
	private static final long BARTERER_MEMORY_TICKS = 20L * 60L;
	private static final Map<Piglin, BartererMemory> LAST_BARTERERS = new WeakHashMap<>();

	private PiglinRules() {
	}

	public static boolean shouldGiveRootedDirt(Piglin piglin) {
		return piglin.getRandom().nextFloat() < ROOTED_DIRT_BARTER_CHANCE;
	}

	public static List<ItemStack> rootedDirtBarterResult() {
		return Collections.singletonList(new ItemStack(Blocks.ROOTED_DIRT, 1));
	}

	public static boolean isBarterCurrency(ItemStack stack) {
		return stack.is(Items.GOLD_INGOT);
	}

	public static void rememberBarterer(Piglin piglin, Entity entity) {
		LAST_BARTERERS.remove(piglin);
		if (!(piglin.level() instanceof ServerLevel level) || !(entity instanceof ServerPlayer player)) {
			return;
		}

		LAST_BARTERERS.put(piglin, new BartererMemory(player.getUUID(), level.getGameTime() + BARTERER_MEMORY_TICKS));
	}

	public static void clearBarterer(Piglin piglin) {
		LAST_BARTERERS.remove(piglin);
	}

	public static void grantPoopBarterAdvancement(Piglin piglin) {
		if (!(piglin.level() instanceof ServerLevel level)) {
			return;
		}

		BartererMemory memory = LAST_BARTERERS.remove(piglin);
		if (memory == null || memory.expiresAt < level.getGameTime()) {
			return;
		}

		ServerPlayer player = level.getServer().getPlayerList().getPlayer(memory.playerUuid);
		if (player != null) {
			AdvancementRules.grantPiglinPoopBarter(player);
		}
	}

	private record BartererMemory(UUID playerUuid, long expiresAt) {
	}
}
