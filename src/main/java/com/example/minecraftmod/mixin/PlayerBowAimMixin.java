package com.example.minecraftmod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BowItem.class)
public abstract class PlayerBowAimMixin {
	@Redirect(
		method = "shootProjectile",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"
		)
	)
	private void minecraftmod$shootBowWithoutInaccuracy(Projectile projectile, Entity shooter, float xRot, float yRot, float z, float velocity, float inaccuracy) {
		projectile.shootFromRotation(shooter, xRot, yRot, z, velocity, shooter instanceof LivingEntity ? 0.0F : inaccuracy);
	}
}
