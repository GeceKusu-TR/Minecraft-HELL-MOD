package com.example.minecraftmod.mixin;

import com.example.minecraftmod.DragonRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonChargePlayerPhase.class)
public abstract class DragonChargePlayerPhaseMixin {
	@Shadow
	public abstract void setTarget(Vec3 target);

	@Inject(method = "doServerTick", at = @At("HEAD"))
	private void minecraftmod$retargetChargePhase(ServerLevel level, CallbackInfo ci) {
		if (!DragonRules.isEndDragonAggressionEnabled(level)) {
			return;
		}

		EnderDragon dragon = ((AbstractDragonPhaseInstanceAccessor)(Object)this).minecraftmod$getDragon();
		ServerPlayer target = DragonRules.findNearestDragonTarget(level, dragon.position());
		if (target == null) {
			return;
		}

		this.setTarget(target.getEyePosition());
	}
}
