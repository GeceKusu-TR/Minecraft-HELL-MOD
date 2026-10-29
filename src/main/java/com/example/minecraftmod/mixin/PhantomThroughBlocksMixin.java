package com.example.minecraftmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomThroughBlocksMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void minecraftmod$enableNoPhysics(CallbackInfo ci) {
		((EntityAccessor)(Object)this).minecraftmod$setNoPhysics(true);
	}

	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$ignoreLineOfSightForPlayers(ServerLevel level, LivingEntity target, TargetingConditions conditions, CallbackInfoReturnable<Boolean> cir) {
		if (target instanceof ServerPlayer player) {
			cir.setReturnValue(player.isAlive() && !player.isCreative() && !player.isSpectator()
				&& level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL);
		}
	}
}
