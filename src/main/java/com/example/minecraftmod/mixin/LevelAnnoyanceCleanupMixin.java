package com.example.minecraftmod.mixin;

import java.util.ArrayDeque;
import java.util.Deque;

import com.example.minecraftmod.WorldAnnoyanceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelAnnoyanceCleanupMixin {
	@Unique
	private final ThreadLocal<Deque<BlockState>> minecraftmod$oldAnnoyanceStates = ThreadLocal.withInitial(ArrayDeque::new);

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
	private void minecraftmod$captureOldAnnoyanceState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		Level level = (Level)(Object)this;
		minecraftmod$oldAnnoyanceStates.get().push(level.getBlockState(pos));
	}

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
	private void minecraftmod$clearStaleAnnoyanceState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		Deque<BlockState> oldStates = minecraftmod$oldAnnoyanceStates.get();
		BlockState oldState = oldStates.isEmpty() ? null : oldStates.pop();
		if (oldStates.isEmpty()) {
			minecraftmod$oldAnnoyanceStates.remove();
		}
		if (!cir.getReturnValueZ() || oldState == null) {
			return;
		}

		Level level = (Level)(Object)this;
		WorldAnnoyanceRules.onBlockStateChanged(level, pos, oldState, level.getBlockState(pos));
	}
}
