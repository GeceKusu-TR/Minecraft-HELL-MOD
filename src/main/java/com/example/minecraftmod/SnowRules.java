package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SnowRules {
	public static final int SNOW_LINE_HEIGHT = 160;
	public static final int THICK_SNOW_HEIGHT = 180;
	public static final int APPLY_RADIUS_CHUNKS = 18;
	public static final int CHUNK_SIZE = 16;

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
