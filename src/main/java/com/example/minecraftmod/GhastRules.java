package com.example.minecraftmod;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GhastRules {
	private static final int OVERWORLD_SPAWN_INTERVAL_TICKS = 200;
	private static final int OVERWORLD_SPAWN_RADIUS = 48;
	private static final int OVERWORLD_TARGET_RADIUS = 96;
	private static final int MAX_GHASTS_NEAR_PLAYER = 3;
	private static final int SPAWN_ATTEMPTS = 12;

	private GhastRules() {
	}

	public static boolean isGhastFireball(LargeFireball fireball) {
		Entity owner = fireball.getOwner();
		return owner instanceof Ghast;
	}

	public static boolean shouldBlockNaturalSpawn(EntityType<?> entityType, ServerLevelAccessor level) {
		return entityType == EntityType.GHAST && level.getLevel().dimension() == Level.NETHER;
	}

	public static boolean shouldBlockNaturalSpawn(EntityType<?> entityType, Level level) {
		return entityType == EntityType.GHAST && level.dimension() == Level.NETHER;
	}

	public static boolean shouldBlockEntitySpawn(Entity entity, Level level) {
		return entity.getType() == EntityType.GHAST && level.dimension() == Level.NETHER;
	}

	public static boolean canSeeTarget(Ghast ghast, ServerPlayer player) {
		double targetRadius = DifficultyScalingRules.more(OVERWORLD_TARGET_RADIUS);
		if (!isValidTarget(player) || ghast.level() != player.level() || ghast.distanceToSqr(player) > targetRadius * targetRadius) {
			return false;
		}

		Vec3 start = ghast.getEyePosition();
		Vec3 end = player.getEyePosition();
		return VisibilityRules.canSeeThroughBlocksAndLava(ghast.level(), ghast, start, end);
	}

	public static void tick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}

		retargetOverworldGhasts(level);
		if (level.getGameTime() % DifficultyScalingRules.sooner(OVERWORLD_SPAWN_INTERVAL_TICKS) == 0L) {
			spawnAroundPlayers(level);
		}
	}

	private static void retargetOverworldGhasts(ServerLevel level) {
		Set<UUID> processedGhasts = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			if (!isValidTarget(player)) {
				continue;
			}

			AABB area = player.getBoundingBox().inflate(DifficultyScalingRules.more(OVERWORLD_TARGET_RADIUS));
			for (Ghast ghast : level.getEntities(EntityType.GHAST, area, ghast -> ghast.isAlive())) {
				if (!processedGhasts.add(ghast.getUUID())) {
					continue;
				}

				ServerPlayer currentTarget = ghast.getTarget() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
				if (currentTarget != null && canSeeTarget(ghast, currentTarget)) {
					continue;
				}

			ServerPlayer target = nearestVisiblePlayer(level, ghast, (int)DifficultyScalingRules.more(OVERWORLD_TARGET_RADIUS));
				if (target != null) {
					ghast.setTarget(target);
				} else {
					ghast.setTarget(null);
				}
			}
		}
	}

	private static void spawnAroundPlayers(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (!isValidTarget(player) || NoMobCommands.isNoMobEnabled(player) || nearbyGhastCount(level, player) >= DifficultyScalingRules.more(MAX_GHASTS_NEAR_PLAYER)) {
				continue;
			}

			Ghast ghast = createGhastNear(level, player);
			if (ghast == null) {
				continue;
			}

			ghast.setTarget(player);
			level.addFreshEntity(ghast);
		}
	}

	private static Ghast createGhastNear(ServerLevel level, ServerPlayer player) {
		for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
			int spawnRadius = (int)DifficultyScalingRules.more(OVERWORLD_SPAWN_RADIUS);
			int x = player.getBlockX() + level.getRandom().nextInt(spawnRadius * 2 + 1) - spawnRadius;
			int z = player.getBlockZ() + level.getRandom().nextInt(spawnRadius * 2 + 1) - spawnRadius;
			int y = Math.min(level.getMaxY() - 8, player.getBlockY() + 18 + level.getRandom().nextInt(18));
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.isLoaded(pos) || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
				continue;
			}

			Ghast ghast = EntityType.GHAST.create(level, EntitySpawnReason.NATURAL);
			if (ghast == null) {
				return null;
			}

			ghast.setPos(x + 0.5D, y + 0.5D, z + 0.5D);
			if (level.noCollision(ghast) && canSeeTarget(ghast, player)) {
				return ghast;
			}
		}

		return null;
	}

	private static int nearbyGhastCount(ServerLevel level, ServerPlayer player) {
			AABB area = player.getBoundingBox().inflate(DifficultyScalingRules.more(OVERWORLD_SPAWN_RADIUS));
		List<Ghast> ghasts = level.getEntities(EntityType.GHAST, area, ghast -> ghast.isAlive());
		return ghasts.size();
	}

	private static ServerPlayer nearestVisiblePlayer(ServerLevel level, Ghast ghast, int radius) {
		ServerPlayer nearest = null;
		double maxDistance = radius * radius;
		double nearestDistance = maxDistance;
		for (ServerPlayer player : level.players()) {
			if (!canSeeTarget(ghast, player)) {
				continue;
			}

			double distance = player.distanceToSqr(ghast);
			if (distance <= nearestDistance) {
				nearestDistance = distance;
				nearest = player;
			}
		}

		return nearest;
	}

	private static boolean isValidTarget(ServerPlayer player) {
		return player.isAlive() && !player.isCreative() && !player.isSpectator() && !NoMobCommands.isNoMobEnabled(player);
	}
}
