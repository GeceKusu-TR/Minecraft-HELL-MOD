package com.example.minecraftmod;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.Deque;

public final class SpearRules {
	private static final ThreadLocal<Deque<Player>> LUNGE_PLAYERS = ThreadLocal.withInitial(ArrayDeque::new);

	private SpearRules() {
	}

	public static boolean isSpear(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(Items.TRIDENT) || stack.is(ItemTags.SPEARS));
	}

	public static void beginLunge(Player player) {
		LUNGE_PLAYERS.get().push(player);
	}

	public static void endLunge() {
		Deque<Player> players = LUNGE_PLAYERS.get();
		if (players.isEmpty()) {
			LUNGE_PLAYERS.remove();
			return;
		}

		players.pop();
		if (players.isEmpty()) {
			LUNGE_PLAYERS.remove();
			return;
		}
	}

	public static boolean isLunging() {
		return !LUNGE_PLAYERS.get().isEmpty();
	}

	public static Player lungePlayer() {
		return LUNGE_PLAYERS.get().peek();
	}
}
