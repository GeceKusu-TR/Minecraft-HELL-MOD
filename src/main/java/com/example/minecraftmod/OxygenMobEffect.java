package com.example.minecraftmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class OxygenMobEffect extends MobEffect {
	public OxygenMobEffect() {
		super(MobEffectCategory.HARMFUL, 0xD10000);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatableWithFallback(getDescriptionId(), "Oksijen Yok");
	}
}
