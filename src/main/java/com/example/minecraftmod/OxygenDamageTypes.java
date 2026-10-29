package com.example.minecraftmod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class OxygenDamageTypes {
	public static final ResourceKey<DamageType> OXYGEN_DEPRIVATION =
		ResourceKey.create(Registries.DAMAGE_TYPE, MinecraftMod.id("oxygen_deprivation"));

	private OxygenDamageTypes() {
	}

	public static DamageSource oxygenDeprivation(Level level) {
		try {
			Holder<DamageType> damageType = level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(OXYGEN_DEPRIVATION);
			return new DamageSource(damageType);
		} catch (IllegalStateException ex) {
			return level.damageSources().starve();
		}
	}
}
