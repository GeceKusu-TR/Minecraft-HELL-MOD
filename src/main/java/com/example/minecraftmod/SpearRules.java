package com.example.minecraftmod;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public final class SpearRules {
	private static final ThreadLocal<Integer> LUNGE_DEPTH = ThreadLocal.withInitial(() -> 0);
	private static final ThreadLocal<Player> LUNGE_PLAYER = new ThreadLocal<>();

	private SpearRules() {
	}

	public static boolean isSpear(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(Items.TRIDENT) || stack.is(ItemTags.SPEARS));
	}

	public static void beginLunge(Player player) {
		LUNGE_DEPTH.set(LUNGE_DEPTH.get() + 1);
		LUNGE_PLAYER.set(player);
	}

	public static void endLunge() {
		int depth = LUNGE_DEPTH.get();
		if (depth <= 0) {
			LUNGE_DEPTH.remove();
			LUNGE_PLAYER.remove();
			return;
		}

		depth--;
		if (depth <= 0) {
			LUNGE_DEPTH.remove();
			LUNGE_PLAYER.remove();
			return;
		}

		LUNGE_DEPTH.set(depth);
	}

	public static boolean isLunging() {
		return LUNGE_DEPTH.get() > 0;
	}

	public static Player lungePlayer() {
		return LUNGE_PLAYER.get();
	}
}
