package com.example.minecraftmod.mixin;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.server.commands.FillCommand.class)
public abstract class FillCommandMixin {
	@Redirect(
		method = "fillBlocks",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;")
	)
	private static Object minecraftmod$removeFillBlockLimit(GameRules gameRules, GameRule<Integer> rule) {
		if (rule == GameRules.MAX_BLOCK_MODIFICATIONS) {
			return Integer.MAX_VALUE;
		}
		return gameRules.get(rule);
	}

	@Redirect(
		method = {
			"method_65985",
			"method_13346",
			"method_65993",
			"method_65995",
			"method_65998",
			"method_65999"
		},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/coordinates/BlockPosArgument;getLoadedBlockPos(Lcom/mojang/brigadier/context/CommandContext;Ljava/lang/String;)Lnet/minecraft/core/BlockPos;")
	)
	private static BlockPos minecraftmod$allowFillUnloadedPositions(CommandContext<CommandSourceStack> context, String name) {
		return BlockPosArgument.getBlockPos(context, name);
	}

	@Redirect(
		method = "fillBlocks",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;")
	)
	private static BlockState minecraftmod$loadChunkBeforeFillReads(ServerLevel level, BlockPos pos) {
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		return level.getBlockState(pos);
	}

	@Redirect(
		method = "fillBlocks",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/blocks/BlockInput;place(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)Z")
	)
	private static boolean minecraftmod$loadChunkBeforeFillPlaces(BlockInput input, ServerLevel level, BlockPos pos, int flags) {
		level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		return input.place(level, pos, flags);
	}
}
