package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.Predicate;

public final class OverworldLavaRules {
	private static final int SECTION_SIZE = 16;
	private static final Predicate<BlockState> HAS_WATER = OverworldLavaRules::containsWater;
	private static final int REPLACE_FLAGS = Block.UPDATE_CLIENTS
		| Block.UPDATE_KNOWN_SHAPE
		| Block.UPDATE_SUPPRESS_DROPS
		| Block.UPDATE_SKIP_ON_PLACE;

	private OverworldLavaRules() {
	}

	public static boolean isOverworld(Level level) {
		return level.dimension() == Level.OVERWORLD;
	}

	public static boolean isWater(BlockState state) {
		return state.is(Blocks.WATER);
	}

	public static boolean containsWater(BlockState state) {
		return state.getFluidState().is(FluidTags.WATER);
	}

	public static boolean isLava(BlockState state) {
		return state.is(Blocks.LAVA);
	}

	public static BlockState overworldLava() {
		return Blocks.LAVA.defaultBlockState();
	}

	public static boolean placeLavaInsteadOfWater(Level level, BlockPos pos, int maxUpdateDepth) {
		return level.setBlock(pos, overworldLava(), REPLACE_FLAGS, maxUpdateDepth);
	}

	public static boolean placeWithoutHiddenWater(Level level, BlockPos pos, BlockState state, int maxUpdateDepth) {
		if (isWater(state)) {
			return placeLavaInsteadOfWater(level, pos, maxUpdateDepth);
		}

		if (state.hasProperty(BlockStateProperties.WATERLOGGED) && Boolean.TRUE.equals(state.getValue(BlockStateProperties.WATERLOGGED))) {
			return level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), REPLACE_FLAGS, maxUpdateDepth);
		}

		if (containsWater(state)) {
			return placeLavaInsteadOfWater(level, pos, maxUpdateDepth);
		}

		return level.setBlock(pos, state, REPLACE_FLAGS, maxUpdateDepth);
	}

	public static void replaceWaterInChunk(ServerLevel level, LevelChunk chunk) {
		if (!isOverworld(level)) {
			return;
		}

		LevelChunkSection[] sections = chunk.getSections();
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		int remainingReplacements = DifficultyScalingRules.more(PerformanceRules.OVERWORLD_WATER_TO_LAVA_REPLACEMENTS_PER_CHUNK_TICK);

		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			LevelChunkSection section = sections[sectionIndex];
			if (section == null || section.hasOnlyAir() || !section.maybeHas(HAS_WATER)) {
				continue;
			}

			int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
			int baseY = SectionPos.sectionToBlockCoord(sectionY);
			remainingReplacements = replaceWaterInSection(level, section, mutablePos, baseX, baseY, baseZ, remainingReplacements);
			if (remainingReplacements <= 0) {
				return;
			}
		}
	}

	public static boolean convertWaterOrPreventStone(Level level, BlockPos pos, BlockState state) {
		if (!isOverworld(level)) {
			return false;
		}

		if (containsWater(state)) {
			placeWithoutHiddenWater(level, pos, state, 512);
			return true;
		}

		if (!isLava(state)) {
			return false;
		}

		return replaceTouchingWater(level, pos);
	}

	private static boolean replaceTouchingWater(Level level, BlockPos pos) {
		boolean touchedWater = false;
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.relative(direction);
			BlockState neighborState = level.getBlockState(neighborPos);
			if (!level.getFluidState(neighborPos).is(FluidTags.WATER)) {
				continue;
			}

			touchedWater = true;
			placeWithoutHiddenWater(level, neighborPos, neighborState, 512);
		}

		return touchedWater;
	}

	private static int replaceWaterInSection(
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
					if (!containsWater(state)) {
						continue;
					}

					mutablePos.set(baseX + localX, baseY + localY, baseZ + localZ);
					placeWithoutHiddenWater(level, mutablePos, state, 512);
					remainingReplacements--;
					if (remainingReplacements <= 0) {
						return 0;
					}
				}
			}
		}

		return remainingReplacements;
	}
}
