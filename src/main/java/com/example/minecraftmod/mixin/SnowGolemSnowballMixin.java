package com.example.minecraftmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Snowball.class)
public abstract class SnowGolemSnowballMixin {
	private static final float FOUR_HEART_DAMAGE = 8.0F;
	private static final float NORMAL_DAMAGE = 1.0F;
	private static final float FOUR_HEART_CHANCE = 0.10F;

	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$allowSnowGolemSnowballsOnlyAtPlayers(EntityHitResult hitResult, CallbackInfo ci) {
		Snowball snowball = (Snowball)(Object)this;
		if (!(snowball.getOwner() instanceof SnowGolem owner)) {
			return;
		}

		ci.cancel();
		if (!(snowball.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		DamageSource source = level.damageSources().thrown(snowball, owner);
		float damage = level.random.nextFloat() < FOUR_HEART_CHANCE ? FOUR_HEART_DAMAGE : NORMAL_DAMAGE;
		player.hurtServer(level, source, damage);
	}
}
