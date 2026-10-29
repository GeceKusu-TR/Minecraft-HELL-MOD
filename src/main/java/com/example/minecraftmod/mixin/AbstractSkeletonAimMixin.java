package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SkeletonTargetingRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonAimMixin {
	@Inject(method = "createAttributes", at = @At("RETURN"))
	private static void minecraftmod$extendSkeletonFollowRange(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		cir.getReturnValue().add(Attributes.FOLLOW_RANGE, 50.0D);
	}

	@Redirect(
		method = "performRangedAttack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"
		)
	)
	private Projectile minecraftmod$aimArrowAtTarget(
		Projectile projectile,
		ServerLevel level,
		ItemStack projectileStack,
		double x,
		double y,
		double z,
		float velocity,
		float inaccuracy
	) {
		AbstractSkeleton skeleton = (AbstractSkeleton)(Object)this;
		if (skeleton.getTarget() instanceof ServerPlayer player && !SkeletonTargetingRules.canSeeTarget(skeleton, player)) {
			skeleton.setTarget(null);
			return projectile;
		}

		Projectile result = Projectile.spawnProjectileUsingShoot(
			projectile,
			level,
			projectileStack,
			x,
			y,
			z,
			Math.max(velocity, 3.0F),
			0.0F
		);

		if (result instanceof AbstractArrow arrow) {
			arrow.setNoGravity(true);
			Vec3 motion = arrow.getDeltaMovement();
			if (motion.lengthSqr() > 1.0E-4D) {
				arrow.setPos(
					arrow.getX() + motion.x * 0.25D,
					arrow.getY() + motion.y * 0.25D,
					arrow.getZ() + motion.z * 0.25D
				);
			}
		}

		return result;
	}

	@ModifyConstant(method = "registerGoals", constant = @Constant(floatValue = 8.0F))
	private float minecraftmod$extendSkeletonTargetRange(float original) {
		return 50.0F;
	}
}
