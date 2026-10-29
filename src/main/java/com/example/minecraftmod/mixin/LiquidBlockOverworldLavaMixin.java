package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OverworldLavaRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockOverworldLavaMixin {
	@Inject(method = "shouldSpreadLiquid", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$replaceOverworldWaterAndPreventStone(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (OverworldLavaRules.convertWaterOrPreventStone(level, pos, state)) {
			cir.setReturnValue(OverworldLavaRules.isLava(state));
		}
	}
}
