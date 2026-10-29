package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AnimalAttackRules;
import com.example.minecraftmod.NoMobCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class AnimalAttackMixin {
	@Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$removeNoMobAttacker(CallbackInfo ci) {
		Mob mob = (Mob)(Object)this;
		if (mob.getTarget() instanceof ServerPlayer player && NoMobCommands.isNoMobEnabled(player)) {
			mob.setTarget(null);
			mob.discard();
			ci.cancel();
		}
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void minecraftmod$attackPlayers(CallbackInfo ci) {
		if (!((Object)this instanceof Animal animal)) {
			return;
		}

		MonsterAccessor accessor = (MonsterAccessor)(Object)this;
		accessor.minecraftmod$getGoalSelector().addGoal(2, new MeleeAttackGoal(animal, 1.2D, true));
		accessor.minecraftmod$getTargetSelector().addGoal(1, new NearestAttackableTargetGoal<Player>(
			animal,
			Player.class,
			10,
			true,
			false,
			(LivingEntity livingEntity, net.minecraft.server.level.ServerLevel level) ->
				livingEntity instanceof Player player && AnimalAttackRules.canTarget(player)
		));
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void minecraftmod$tickAngryAnimal(CallbackInfo ci) {
		if ((Object)this instanceof Animal animal) {
			AnimalAttackRules.tickAngryAnimal(animal);
		}
	}
}
