package com.example.minecraftmod;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public final class NetherWaterRules {
	private static final int SECTION_SIZE = 16;
	private static final int MAX_REMEMBERED_CHUNKS = 32768;
	private static final Map<ServerLevel, LongSet> PROCESSED_NETHER_CHUNKS = new WeakHashMap<>();
	private static final Predicate<BlockState> HAS_LAVA = NetherWaterRules::isLava;
	private static final int REPLACE_FLAGS = Block.UPDATE_CLIENTS
		| Block.UPDATE_KNOWN_SHAPE
		| Block.UPDATE_SUPPRESS_DROPS
		| Block.UPDATE_SKIP_ON_PLACE;

	private NetherWaterRules() {
	}

	public static boolean isNether(Level level) {
		return level.dimension() == Level.NETHER;
	}

	public static boolean isLava(BlockState state) {
		return state.is(Blocks.LAVA);
	}

	public static BlockState netherWater() {
		return Blocks.WATER.defaultBlockState();
	}

	public static boolean placeWaterInsteadOfLava(Level level, BlockPos pos, int flags, int maxUpdateDepth) {
		return level.setBlock(pos, netherWater(), flags, maxUpdateDepth);
	}

	public static void replaceLavaInChunk(ServerLevel level, LevelChunk chunk) {
		if (!isNether(level)) {
			return;
		}

		LongSet processedChunks = PROCESSED_NETHER_CHUNKS.computeIfAbsent(level, ignored -> new LongOpenHashSet());
		long chunkKey = chunk.getPos().toLong();
		if (processedChunks.contains(chunkKey)) {
			return;
		}

		if (processedChunks.size() > MAX_REMEMBERED_CHUNKS) {
			processedChunks.clear();
		}

		LevelChunkSection[] sections = chunk.getSections();
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		int remainingReplacements = DifficultyScalingRules.more(512);

		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			LevelChunkSection section = sections[sectionIndex];
			if (section == null || section.hasOnlyAir() || !section.maybeHas(HAS_LAVA)) {
				continue;
			}

			int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
			int baseY = SectionPos.sectionToBlockCoord(sectionY);
			remainingReplacements = replaceLavaInSection(level, section, mutablePos, baseX, baseY, baseZ, remainingReplacements);
			if (remainingReplacements == 0) {
				return;
			}
		}
		processedChunks.add(chunkKey);
	}

	private static int replaceLavaInSection(
		ServerLevel level,
		LevelChunkSection section,
		BlockPos.MutableBlockPos mutablePos,
		int baseX,
		int baseY,
		int baseZ,
		int remainingReplacements
	) {
		for (int localY = 0; localY < SECTION_SIZE; localY++) {
			for (int localZ = 0; localZ < SECTION_SIZE; localZ++) {
				for (int localX = 0; localX < SECTION_SIZE; localX++) {
					BlockState state = section.getBlockState(localX, localY, localZ);
					if (!isLava(state)) {
						continue;
					}

					mutablePos.set(baseX + localX, baseY + localY, baseZ + localZ);
					level.setBlock(mutablePos, netherWater(), REPLACE_FLAGS);
					if (--remainingReplacements == 0) {
						return 0;
					}
				}
			}
		}
		return remainingReplacements;
	}
}
