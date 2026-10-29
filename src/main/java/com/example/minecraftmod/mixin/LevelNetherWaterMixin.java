package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NetherWaterRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelNetherWaterMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$replaceNetherLavaOnPlacement(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		Level level = (Level)(Object)this;
		if (NetherWaterRules.isNether(level) && NetherWaterRules.isLava(state)) {
			cir.setReturnValue(NetherWaterRules.placeWaterInsteadOfLava(level, pos, flags, maxUpdateDepth));
		}
	}
}
