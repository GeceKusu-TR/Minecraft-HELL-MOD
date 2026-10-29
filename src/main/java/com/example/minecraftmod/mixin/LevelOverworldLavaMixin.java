package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OverworldLavaRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelOverworldLavaMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$replaceOverworldWaterOnPlacement(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		Level level = (Level)(Object)this;
		if (OverworldLavaRules.isOverworld(level) && OverworldLavaRules.containsWater(state)) {
			cir.setReturnValue(OverworldLavaRules.placeWithoutHiddenWater(level, pos, state, maxUpdateDepth));
		}
	}
}
