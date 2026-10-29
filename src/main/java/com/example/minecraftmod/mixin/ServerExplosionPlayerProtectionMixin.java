package com.example.minecraftmod.mixin;

import com.example.minecraftmod.TntRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionPlayerProtectionMixin {
	@Shadow
	@Final
	private Entity source;

	@WrapOperation(
		method = "calculateExplodedPositions",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;shouldBlockExplode(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)Z")
	)
	private boolean minecraftmod$protectOresFromVanillaTntExplosion(
		ExplosionDamageCalculator calculator,
		Explosion explosion,
		BlockGetter level,
		BlockPos pos,
		BlockState state,
		float power,
		Operation<Boolean> original
	) {
		if (TntRules.shouldProtectBlockFromTntExplosion(this.source, state)) {
			return false;
		}
		return original.call(calculator, explosion, level, pos, state, power);
	}

	@WrapOperation(
		method = "hurtEntities",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z")
	)
	private boolean minecraftmod$blockTntDamageToPlayers(Entity target, ServerLevel level, DamageSource damageSource, float amount, Operation<Boolean> original) {
		if (TntRules.shouldIgnoreExplosionForPlayer(this.source, target)) {
			return false;
		}
		return original.call(target, level, damageSource, amount);
	}

	@WrapOperation(
		method = "hurtEntities",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V")
	)
	private void minecraftmod$blockTntKnockbackToPlayers(Entity target, Vec3 movement, Operation<Void> original) {
		if (TntRules.shouldIgnoreExplosionForPlayer(this.source, target)) {
			return;
		}
		original.call(target, movement);
	}
}
