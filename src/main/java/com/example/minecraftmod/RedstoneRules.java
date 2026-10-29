package com.example.minecraftmod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RedstoneRules {
	private static final int MAX_WIRE_SEARCH = 4096;

	private RedstoneRules() {
	}

	public static int stableWireSignal(Level level, BlockPos pos) {
		if (!level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) {
			return level.getBestNeighborSignal(pos);
		}

		int externalSignal = strongestExternalSignal(level, pos);
		if (externalSignal >= 15) {
			return 15;
		}

		int wireSignal = hasPoweredWireNetworkSource(level, pos) ? Math.max(0, strongestNeighborWireSignal(level, pos) - 1) : 0;
		return Math.max(externalSignal, wireSignal);
	}

	private static boolean hasPoweredWireNetworkSource(Level level, BlockPos start) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(start.immutable());
		seen.add(start.immutable());

		while (!queue.isEmpty() && seen.size() <= MAX_WIRE_SEARCH) {
			BlockPos pos = queue.removeFirst();
			if (strongestExternalSignal(level, pos) > 0) {
				return true;
			}

			for (Direction direction : Direction.Plane.HORIZONTAL) {
				addWireIfPresent(level, pos.relative(direction), queue, seen);
				addWireIfPresent(level, pos.relative(direction).above(), queue, seen);
				addWireIfPresent(level, pos.relative(direction).below(), queue, seen);
			}
		}

		return false;
	}

	private static void addWireIfPresent(Level level, BlockPos pos, ArrayDeque<BlockPos> queue, Set<BlockPos> seen) {
		if (seen.size() > MAX_WIRE_SEARCH || !level.getBlockState(pos).is(Blocks.REDSTONE_WIRE)) {
			return;
		}

		BlockPos immutable = pos.immutable();
		if (seen.add(immutable)) {
			queue.add(immutable);
		}
	}

	private static int strongestNeighborWireSignal(Level level, BlockPos pos) {
		int signal = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			signal = Math.max(signal, wireSignalAt(level, pos.relative(direction)));
			signal = Math.max(signal, wireSignalAt(level, pos.relative(direction).above()));
			signal = Math.max(signal, wireSignalAt(level, pos.relative(direction).below()));
		}
		return signal;
	}

	private static int wireSignalAt(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(Blocks.REDSTONE_WIRE) ? state.getValue(RedStoneWireBlock.POWER) : 0;
	}

	private static int strongestExternalSignal(Level level, BlockPos pos) {
		int signal = 0;
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.relative(direction);
			BlockState neighborState = level.getBlockState(neighborPos);
			if (neighborState.is(Blocks.REDSTONE_WIRE)) {
				continue;
			}

			signal = Math.max(signal, neighborState.getSignal(level, neighborPos, direction));
			if (neighborState.isRedstoneConductor(level, neighborPos)) {
				signal = Math.max(signal, directSignalToWithoutWire(level, neighborPos));
			}
		}
		return signal;
	}

	private static int directSignalToWithoutWire(Level level, BlockPos pos) {
		return Math.max(
			Math.max(
				getDirectSignalWithoutWire(level, pos.below(), Direction.DOWN),
				getDirectSignalWithoutWire(level, pos.above(), Direction.UP)
			),
			Math.max(
				Math.max(
					getDirectSignalWithoutWire(level, pos.north(), Direction.NORTH),
					getDirectSignalWithoutWire(level, pos.south(), Direction.SOUTH)
				),
				Math.max(
					getDirectSignalWithoutWire(level, pos.west(), Direction.WEST),
					getDirectSignalWithoutWire(level, pos.east(), Direction.EAST)
				)
			)
		);
	}

	private static int getDirectSignalWithoutWire(SignalGetter level, BlockPos pos, Direction direction) {
		BlockState state = level.getBlockState(pos);
		return state.is(Blocks.REDSTONE_WIRE) ? 0 : state.getDirectSignal(level, pos, direction);
	}
}
