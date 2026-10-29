package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GhastRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastShootFireballGoal")
public abstract class GhastShootFireballGoalMixin {
	@Shadow
	@Final
	private Ghast ghast;

	@Shadow
	public int chargeTime;

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$stopShootingThroughWallsAndLava(CallbackInfo ci) {
		LivingEntity target = this.ghast.getTarget();
		if (!(target instanceof ServerPlayer player) || GhastRules.canSeeTarget(this.ghast, player)) {
			return;
		}

		if (this.chargeTime > 0) {
			this.chargeTime--;
		}
		this.ghast.setCharging(false);
		this.ghast.setTarget(null);
		ci.cancel();
	}
}
