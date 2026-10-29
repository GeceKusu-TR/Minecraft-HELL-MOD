package com.example.minecraftmod;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.WeakHashMap;

public final class SnowPerformanceRules {
	private static final int MAX_PROCESSED_CHUNKS_PER_LEVEL = 32768;
	private static final int PLAYER_AREA_SCAN_INTERVAL_TICKS = 20;
	private static final Map<ServerLevel, LongSet> PROCESSED_SNOW_CHUNKS = new WeakHashMap<>();

	private SnowPerformanceRules() {
	}

	public static boolean shouldScanPlayerArea(ServerLevel level) {
		return PerformanceRules.shouldRunEveryTicks(level, "snow_player_area", PLAYER_AREA_SCAN_INTERVAL_TICKS);
	}

	public static boolean markLoadedChunkForProcessing(ServerLevel level, int chunkX, int chunkZ) {
		LongSet processedChunks = PROCESSED_SNOW_CHUNKS.computeIfAbsent(level, ignored -> new LongOpenHashSet());
		long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
		if (!processedChunks.add(chunkKey)) {
			return false;
		}

		if (processedChunks.size() > MAX_PROCESSED_CHUNKS_PER_LEVEL) {
			processedChunks.clear();
			processedChunks.add(chunkKey);
		}

		return true;
	}
}
