package com.example.minecraftmod.mixin;

import com.example.minecraftmod.DragonRules;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonAggroMixin {
	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$forcePlayerAggro(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		EnderDragon dragon = (EnderDragon)(Object)this;
		if (dragon.level().getDifficulty() == Difficulty.PEACEFUL) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(target instanceof Player player && DragonRules.isEligibleDragonTarget(player));
	}
}
