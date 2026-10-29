package com.example.minecraftmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class NetherRules {
	public static final float ENDERMAN_PEARL_DROP_CHANCE = 0.01F;
	public static final int GUARANTEED_BLAZE_ROD_COUNT = 1;
	public static final int BLAZE_POWDER_PER_ROD = 1;

	private NetherRules() {
	}

	public static boolean disablesSprintSlowdown(ServerPlayer player) {
		return player.level().dimension() == Level.NETHER;
	}
}
