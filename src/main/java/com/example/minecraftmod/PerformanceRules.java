package com.example.minecraftmod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class PerformanceRules {
	private static final int MAX_END_ENDERMEN = 20;
	public static final int OVERWORLD_WATER_TO_LAVA_REPLACEMENTS_PER_CHUNK_TICK = 512;
	private static final Map<ServerLevel, Map<String, Long>> LAST_RUN_TICKS = new WeakHashMap<>();

	private PerformanceRules() {
	}

	public static boolean shouldRunEveryTicks(ServerLevel level, String key, int intervalTicks) {
		long gameTime = level.getGameTime();
		Map<String, Long> levelTicks = LAST_RUN_TICKS.computeIfAbsent(level, ignored -> new HashMap<>());
		Long lastRunTick = levelTicks.get(key);
		if (lastRunTick != null && gameTime - lastRunTick < intervalTicks) {
			return false;
		}

		levelTicks.put(key, gameTime);
		return true;
	}

	public static <T extends Entity> boolean isBelowEntityLimit(ServerLevel level, Class<T> entityClass, AABB area, int limit) {
		return level.getEntitiesOfClass(entityClass, area, entity -> true).size() < limit;
	}

	public static boolean canSpawnEndEnderman(EntityType<?> entityType, ServerLevelAccessor level, net.minecraft.core.BlockPos pos) {
		return canSpawnEndEnderman(entityType, level.getLevel(), pos);
	}

	public static boolean canSpawnEndEnderman(EntityType<?> entityType, LevelAccessor level, net.minecraft.core.BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return true;
		}

		return canSpawnEndEnderman(entityType, serverLevel, pos);
	}

	private static boolean canSpawnEndEnderman(EntityType<?> entityType, ServerLevel level, net.minecraft.core.BlockPos pos) {
		if (entityType != EntityType.ENDERMAN || level.dimension() != Level.END) {
			return true;
		}

		return countEndermen(level) < MAX_END_ENDERMEN;
	}

	public static boolean canAddEntityToLevel(ServerLevel level, Entity entity) {
		if (!(entity instanceof EnderMan) || level.dimension() != Level.END) {
			return true;
		}

		if (countEndermen(level) < MAX_END_ENDERMEN) {
			return true;
		}

		removeOneExistingEnderman(level, entity);
		return countEndermen(level) < MAX_END_ENDERMEN;
	}

	public static void enforceEndEndermanLimit(ServerLevel level) {
		if (level.dimension() != Level.END || !shouldRunEveryTicks(level, "end_Enderman_limit", 20)) {
			return;
		}

		int count = 0;
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof EnderMan) || !entity.isAlive()) {
				continue;
			}

			count++;
			if (count > MAX_END_ENDERMEN) {
				entity.discard();
			}
		}
	}

	private static int countEndermen(ServerLevel level) {
		int count = 0;
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof EnderMan && entity.isAlive()) {
				count++;
				if (count >= MAX_END_ENDERMEN) {
					return count;
				}
			}
		}

		return count;
	}

	private static void removeOneExistingEnderman(ServerLevel level, Entity incomingEntity) {
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof EnderMan && entity.isAlive() && entity != incomingEntity) {
				entity.discard();
				return;
			}
		}
	}
}
