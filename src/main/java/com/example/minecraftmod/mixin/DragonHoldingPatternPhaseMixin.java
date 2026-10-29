package com.example.minecraftmod.mixin;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DragonHoldingPatternPhase.class)
public abstract class DragonHoldingPatternPhaseMixin {
	@ModifyArg(method = "findNewTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0), index = 0)
	private int minecraftmod$landMuchMoreOften(int original) {
		return Math.max(1, original / 20);
	}
}
