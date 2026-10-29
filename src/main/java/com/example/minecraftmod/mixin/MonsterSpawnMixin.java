package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GhastRules;
import com.example.minecraftmod.NoMobCommands;
import com.example.minecraftmod.PerformanceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public abstract class MonsterSpawnMixin {
	@Inject(method = "isDarkEnoughToSpawn", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$allowSpawnInAnyLight(ServerLevelAccessor level, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}

	@Inject(method = "checkMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$allowMonsterSpawn(EntityType<?> entityType, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (!PerformanceRules.canSpawnEndEnderman(entityType, level, pos)) {
			cir.setReturnValue(false);
			return;
		}

		if (GhastRules.shouldBlockNaturalSpawn(entityType, level)) {
			cir.setReturnValue(false);
			return;
		}

		if (NoMobCommands.shouldBlockHostileSpawn(level.getLevel(), pos)) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}

	@Inject(method = "checkAnyLightMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$allowAnyLightMonsterSpawn(EntityType<?> entityType, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (!PerformanceRules.canSpawnEndEnderman(entityType, level, pos)) {
			cir.setReturnValue(false);
			return;
		}

		if (level instanceof net.minecraft.world.level.Level concreteLevel && GhastRules.shouldBlockNaturalSpawn(entityType, concreteLevel)) {
			cir.setReturnValue(false);
			return;
		}

		if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && NoMobCommands.shouldBlockHostileSpawn(serverLevel, pos)) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}

	@Inject(method = "checkSurfaceMonstersSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$allowSurfaceMonsterSpawn(EntityType<?> entityType, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (!PerformanceRules.canSpawnEndEnderman(entityType, level, pos)) {
			cir.setReturnValue(false);
			return;
		}

		if (GhastRules.shouldBlockNaturalSpawn(entityType, level)) {
			cir.setReturnValue(false);
			return;
		}

		if (NoMobCommands.shouldBlockHostileSpawn(level.getLevel(), pos)) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL);
	}
}
