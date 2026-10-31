package com.example.minecraftmod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SnowGolem.class)
public abstract class SnowGolemAccuracyMixin {
	@WrapOperation(
		method = "method_63010",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/Snowball;shoot(DDDFF)V")
	)
	private static void minecraftmod$removeSnowballSpread(Snowball projectile, double x, double y, double z, float speed, float inaccuracy, Operation<Void> original) {
		original.call(projectile, x, y, z, speed, 3.0F);
	}
}
