package com.example.minecraftmod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class SnowGolemZombieTargetMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$ignoreSnowGolemZombieTargets(LivingEntity target, CallbackInfo ci) {
		Mob mob = (Mob)(Object)this;
		if (isForbiddenPair(mob, target)) {
			ci.cancel();
		}
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void minecraftmod$clearExistingSnowGolemZombieTargets(CallbackInfo ci) {
		Mob mob = (Mob)(Object)this;
		if (isForbiddenPair(mob, mob.getTarget())) {
			mob.setTarget(null);
		}
	}

	private static boolean isForbiddenPair(Mob attacker, LivingEntity target) {
		return (attacker instanceof SnowGolem && target instanceof Zombie)
			|| (attacker instanceof Zombie && target instanceof SnowGolem);
	}
}
