package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AnimalAttackRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobAnimalAttackDamageMixin {
	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$hurtPlayerAsAngryAnimal(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
		if ((Object)this instanceof Animal animal && target instanceof Player player && AnimalAttackRules.canTarget(player)) {
			cir.setReturnValue(AnimalAttackRules.performAnimalAttack(level, animal, player));
		}
	}
}
