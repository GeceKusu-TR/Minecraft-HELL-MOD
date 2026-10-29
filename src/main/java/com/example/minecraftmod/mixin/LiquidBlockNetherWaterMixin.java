package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NetherWaterRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockNetherWaterMixin {
	@Inject(method = "shouldSpreadLiquid", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$keepNetherWaterAndReplaceLava(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (!NetherWaterRules.isNether(level)) {
			return;
		}

		if (state.is(Blocks.WATER)) {
			cir.setReturnValue(true);
			return;
		}

		if (NetherWaterRules.isLava(state)) {
			level.setBlock(pos, NetherWaterRules.netherWater(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
			cir.setReturnValue(false);
		}
	}
}
