package com.example.minecraftmod.mixin;

import com.example.minecraftmod.DragonRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingFlamingPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonSittingFlamingPhase.class)
public abstract class DragonSittingFlamingPhaseMixin {
	@Shadow
	private AreaEffectCloud flame;

	@ModifyConstant(method = "doServerTick", constant = @Constant(intValue = 200))
	private int minecraftmod$fasterFlameCycle(int original) {
		EnderDragon dragon = ((AbstractDragonPhaseInstanceAccessor)(Object)this).minecraftmod$getDragon();
		return DragonRules.flameCycleTicks(dragon);
	}

	@ModifyConstant(method = "doServerTick", constant = @Constant(intValue = 10))
	private int minecraftmod$fasterFlameWarmup(int original) {
		return 3;
	}

	@Inject(
			method = "doServerTick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
					shift = At.Shift.AFTER
			)
	)
	private void minecraftmod$buildObsidianBreathWall(ServerLevel level, CallbackInfo ci) {
		EnderDragon dragon = ((AbstractDragonPhaseInstanceAccessor)(Object)this).minecraftmod$getDragon();
		DragonRules.onDragonBreathCloudCreated(level, dragon, this.flame);
	}
}
