package com.example.minecraftmod.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ApplyStatusEffectsConsumeEffect.class)
public abstract class UnderwaterGoldenAppleMixin {
	@Inject(method = "apply", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$limitGoldenAppleEffects(Level level, ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!(entity instanceof Player)) {
			return;
		}

		if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
			entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 3));
			entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
			entity.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
			entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 2));
			entity.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 4));
			entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 2));
			if (level.getDifficulty() != Difficulty.PEACEFUL) {
				entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 200, 1));
			}
			entity.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 4));
			entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, 1));
			entity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 200, 0));
			entity.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 200, 2));
			cir.setReturnValue(true);
			return;
		}

	}
}
