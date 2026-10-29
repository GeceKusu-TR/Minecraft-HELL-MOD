package com.example.minecraftmod;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public final class OxygenEffects {
	private static final String MOD_ID = "minecraftmod";
	private static boolean bootstrapped;

	public static Holder<MobEffect> OXYGENLESS;
	public static final Identifier OXYGENLESS_SPRITE = MinecraftMod.id("mob_effect/oksijensizlik");

	private OxygenEffects() {
	}

	public static synchronized void bootstrap() {
		if (bootstrapped) {
			return;
		}

		OXYGENLESS = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT,
			Identifier.fromNamespaceAndPath(MOD_ID, "oksijensizlik"),
			new OxygenMobEffect()
		);
		bootstrapped = true;
	}
}
