package com.example.minecraftmod;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SnowRules {
	public static final int SNOW_LINE_HEIGHT = 160;
	public static final int THICK_SNOW_HEIGHT = 180;
	public static final int APPLY_RADIUS_CHUNKS = 18;
	public static final int CHUNK_SIZE = 16;
	private static final int HARDCORE_SNOW_GOLEM_HEIGHT = 200;
	private static final int HARDCORE_SNOW_GOLEM_SPAWN_INTERVAL_TICKS = 20;
	private static final int HARDCORE_SNOW_GOLEM_ATTACK_INTERVAL_TICKS = 20;
	private static final double HARDCORE_SNOW_GOLEM_LOCAL_RADIUS = 16.0D;
	private static final double HARDCORE_SNOW_GOLEM_GLOBAL_RADIUS = 160.0D;
	private static final int HARDCORE_SNOW_GOLEM_LOCAL_LIMIT = 5;
	private static final int HARDCORE_SNOW_GOLEM_GLOBAL_LIMIT = 20;
	private static final int HARDCORE_SNOW_GOLEM_SPAWN_ATTEMPTS = 12;

	private SnowRules() {
	}

	public static void applyToChunk(WorldGenLevel worldGenLevel, ChunkAccess chunk) {
		if (!worldGenLevel.dimensionType().hasSkyLight()) {
			return;
		}

		ChunkPos chunkPos = chunk.getPos();
		int minX = chunkPos.getMinBlockX();
		int minZ = chunkPos.getMinBlockZ();

		for (int localX = 0; localX < CHUNK_SIZE; localX++) {
			for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
				int worldX = minX + localX;
				int worldZ = minZ + localZ;
				placeSnowColumn(worldGenLevel, worldX, worldZ);
			}
		}
	}

	public static void applyAroundPlayers(ServerLevel serverLevel) {
		if (!serverLevel.dimensionType().hasSkyLight()) {
			return;
		}

		if (!SnowPerformanceRules.shouldScanPlayerArea(serverLevel)) {
			return;
		}

		tickHardcoreSnowGolems(serverLevel);

		ChunkSource chunkSource = serverLevel.getChunkSource();
		int remainingChunks = 8;

		for (ServerPlayer player : serverLevel.players()) {
			ChunkPos playerChunk = player.chunkPosition();
			for (int chunkOffsetX = -APPLY_RADIUS_CHUNKS; chunkOffsetX <= APPLY_RADIUS_CHUNKS; chunkOffsetX++) {
				for (int chunkOffsetZ = -APPLY_RADIUS_CHUNKS; chunkOffsetZ <= APPLY_RADIUS_CHUNKS; chunkOffsetZ++) {
					int chunkX = playerChunk.x + chunkOffsetX;
					int chunkZ = playerChunk.z + chunkOffsetZ;
					ChunkAccess chunk = chunkSource.getChunkNow(chunkX, chunkZ);
					if (chunk == null) {
						continue;
					}

					if (!SnowPerformanceRules.markLoadedChunkForProcessing(serverLevel, chunkX, chunkZ)) {
						continue;
					}

					applyToChunk(serverLevel, chunk);
					if (--remainingChunks == 0) {
						return;
					}
				}
			}
		}
	}

	private static void tickHardcoreSnowGolems(ServerLevel level) {
		if (!level.getLevelData().isHardcore()) {
			return;
		}

		spawnHardcoreSnowGolems(level);
		attackNearestPlayers(level);
	}

	private static void spawnHardcoreSnowGolems(ServerLevel level) {
		if (!PerformanceRules.shouldRunEveryTicks(level, "hardcore_snow_golem_spawn", DifficultyScalingRules.sooner(HARDCORE_SNOW_GOLEM_SPAWN_INTERVAL_TICKS))) {
			return;
		}

		for (ServerPlayer player : level.players()) {
			if (!isEligibleHighAltitudePlayer(player) || countSnowGolems(level, player.position(), DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_RADIUS)) >= DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_LIMIT)) {
				continue;
			}

			BlockPos spawnPos = findSnowGolemSpawnPosition(level, player);
			if (spawnPos == null
				|| countSnowGolems(level, spawnPos.getCenter(), DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_LOCAL_RADIUS)) >= DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_LOCAL_LIMIT)
				|| countSnowGolems(level, spawnPos.getCenter(), DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_RADIUS)) >= DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_LIMIT)) {
				continue;
			}

			SnowGolem snowGolem = EntityType.SNOW_GOLEM.create(level, EntitySpawnReason.EVENT);
			if (snowGolem == null) {
				continue;
			}

			snowGolem.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
			if (!level.noCollision(snowGolem)) {
				continue;
			}

			level.addFreshEntity(snowGolem);
		}
	}

	private static void attackNearestPlayers(ServerLevel level) {
		if (!PerformanceRules.shouldRunEveryTicks(level, "hardcore_snow_golem_attack", DifficultyScalingRules.sooner(HARDCORE_SNOW_GOLEM_ATTACK_INTERVAL_TICKS))) {
			return;
		}

		Set<SnowGolem> processed = Collections.newSetFromMap(new IdentityHashMap<>());
		for (ServerPlayer highAltitudePlayer : level.players()) {
			if (!isEligibleHighAltitudePlayer(highAltitudePlayer)) {
				continue;
			}

			AABB area = highAltitudePlayer.getBoundingBox().inflate(DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_RADIUS));
			for (SnowGolem snowGolem : level.getEntitiesOfClass(SnowGolem.class, area, SnowGolem::isAlive)) {
				if (!processed.add(snowGolem)) {
					continue;
				}

			ServerPlayer target = nearestPlayer(level, snowGolem.position());
				if (target == null) {
					continue;
				}

				snowGolem.setTarget(target);
				snowGolem.getLookControl().setLookAt(target, 30.0F, 30.0F);
				snowGolem.performRangedAttack(target, 1.0F);
			}
		}
	}

	private static BlockPos findSnowGolemSpawnPosition(ServerLevel level, ServerPlayer player) {
		RandomSource random = player.getRandom();
		for (int attempt = 0; attempt < DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_SPAWN_ATTEMPTS); attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			double localRadius = DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_LOCAL_RADIUS);
			double globalRadius = DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_RADIUS);
			double distance = localRadius + random.nextDouble() * (globalRadius - localRadius);
			int x = (int)Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int)Math.floor(player.getZ() + Math.sin(angle) * distance);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos spawnPos = new BlockPos(x, y, z);

			if (spawnPos.getY() < HARDCORE_SNOW_GOLEM_HEIGHT
				|| !level.hasChunk(spawnPos.getX() >> 4, spawnPos.getZ() >> 4)
				|| !level.canSeeSky(spawnPos)
				|| !level.isEmptyBlock(spawnPos)
				|| !level.isEmptyBlock(spawnPos.above())
				|| !level.getFluidState(spawnPos).isEmpty()
				|| !level.getFluidState(spawnPos.above()).isEmpty()
				|| !level.getBlockState(spawnPos.below()).isFaceSturdy(level, spawnPos.below(), Direction.UP)) {
				continue;
			}

			return spawnPos;
		}

		return null;
	}

	private static int countSnowGolems(ServerLevel level, Vec3 center, double radius) {
		AABB area = new AABB(center.x, center.y, center.z, center.x, center.y, center.z).inflate(radius);
		return level.getEntitiesOfClass(SnowGolem.class, area, SnowGolem::isAlive).size();
	}

	private static ServerPlayer nearestPlayer(ServerLevel level, Vec3 origin) {
		ServerPlayer nearest = null;
		double globalRadius = DifficultyScalingRules.more(HARDCORE_SNOW_GOLEM_GLOBAL_RADIUS);
		double nearestDistance = globalRadius * globalRadius;

		for (ServerPlayer player : level.players()) {
			if (!player.isAlive() || player.isCreative() || player.isSpectator() || NoMobCommands.isNoMobEnabled(player)) {
				continue;
			}

			double distance = player.distanceToSqr(origin);
			if (distance <= nearestDistance) {
				nearestDistance = distance;
				nearest = player;
			}
		}

		return nearest;
	}

	private static boolean isEligibleHighAltitudePlayer(ServerPlayer player) {
		return player.isAlive()
			&& !player.isCreative()
			&& !player.isSpectator()
			&& !NoMobCommands.isNoMobEnabled(player)
			&& player.getY() >= HARDCORE_SNOW_GOLEM_HEIGHT;
	}

	private static void placeSnowColumn(WorldGenLevel worldGenLevel, int worldX, int worldZ) {
		BlockPos column = new BlockPos(worldX, 0, worldZ);
		BlockPos heightmapPos = worldGenLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
		BlockPos surfacePos = heightmapPos;
		BlockState surfaceState = worldGenLevel.getBlockState(surfacePos);
		if (surfaceState.isAir()) {
			surfacePos = surfacePos.below();
			surfaceState = worldGenLevel.getBlockState(surfacePos);
		}

		int surfaceY = surfacePos.getY();
		if (surfaceY < SNOW_LINE_HEIGHT) {
			return;
		}

		if (surfaceState.is(Blocks.SNOW) || surfaceState.is(Blocks.SNOW_BLOCK)) {
			return;
		}

		if (!surfaceState.isFaceSturdy(worldGenLevel, surfacePos, Direction.UP)) {
			return;
		}

		BlockPos snowPos = surfacePos.above();
		if (!worldGenLevel.canSeeSky(snowPos)) {
			return;
		}

		BlockState snowState = worldGenLevel.getBlockState(snowPos);
		if (!snowState.isAir() && !snowState.canBeReplaced()) {
			return;
		}
		if (surfaceY >= THICK_SNOW_HEIGHT) {
			if (snowState.is(Blocks.SNOW_BLOCK)) {
				return;
			}

			worldGenLevel.setBlock(snowPos, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
			return;
		}

		if (snowState.is(Blocks.SNOW) || snowState.is(Blocks.SNOW_BLOCK)) {
			return;
		}

		if (!snowState.isAir() && !snowState.canBeReplaced()) {
			return;
		}

		if (snowState.isAir() || snowState.canBeReplaced()) {
			worldGenLevel.setBlock(snowPos, Blocks.SNOW.defaultBlockState(), 2);
		}
	}
}
