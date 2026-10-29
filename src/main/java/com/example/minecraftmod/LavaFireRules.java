package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class LavaFireRules {
	private static final int LAVA_PROJECTILE_DAMAGE = 8;
	private static final int IGNITE_SECONDS = 8;
	private static final int LAVA_TRAIL_INTERVAL_TICKS = 2;

	private LavaFireRules() {
	}

	public static void tickGhastLavaFireball(LargeFireball fireball) {
		if (!GhastRules.isGhastFireball(fireball) || fireball.tickCount % LAVA_TRAIL_INTERVAL_TICKS != 0) {
			return;
		}

		Level level = fireball.level();
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.LAVA, fireball.getX(), fireball.getY(), fireball.getZ(), 2, 0.12D, 0.12D, 0.12D, 0.0D);
		}
	}

	public static boolean handleGhastLavaFireballHit(LargeFireball fireball, HitResult hitResult) {
		if (!GhastRules.isGhastFireball(fireball)) {
			return false;
		}

		Level level = fireball.level();
		if (level instanceof ServerLevel serverLevel) {
			if (hitResult instanceof EntityHitResult entityHitResult) {
				hitEntity(serverLevel, entityHitResult.getEntity());
			} else if (hitResult instanceof BlockHitResult blockHitResult) {
				placeLavaAtImpact(serverLevel, blockHitResult);
			}
		}

		fireball.discard();
		return true;
	}

	private static void hitEntity(ServerLevel level, Entity entity) {
		if (entity instanceof LivingEntity livingEntity) {
			if (livingEntity instanceof ServerPlayer serverPlayer) {
				AdvancementRules.markGhastFireballBurn(serverPlayer);
			}

			livingEntity.hurtServer(level, livingEntity.damageSources().lava(), LAVA_PROJECTILE_DAMAGE);
			livingEntity.igniteForSeconds(IGNITE_SECONDS);
		}
	}

	private static void placeLavaAtImpact(ServerLevel level, BlockHitResult hitResult) {
		BlockPos lavaPos = hitResult.getBlockPos().relative(hitResult.getDirection());
		if (!level.isLoaded(lavaPos)) {
			return;
		}

		if (!level.getBlockState(lavaPos).canBeReplaced()) {
			return;
		}

		level.setBlock(lavaPos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_ALL);
	}
}
