package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AnimalAttackRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Player.class)
public abstract class PlayerAnimalDamageMixin {
	@WrapOperation(
		method = "hurtServer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Avatar;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z")
	)
	private boolean minecraftmod$adjustAnimalDamage(Player avatar, ServerLevel level, DamageSource damageSource, float amount, Operation<Boolean> original) {
		Player player = (Player)(Object)this;
		Entity attacker = damageSource.getEntity();
		if (attacker instanceof Animal) {
			amount = AnimalAttackRules.attackDamageFor(player, AnimalAttackRules.UNARMORED_ATTACK_DAMAGE);
		}

		return original.call(avatar, level, damageSource, amount);
	}
}
