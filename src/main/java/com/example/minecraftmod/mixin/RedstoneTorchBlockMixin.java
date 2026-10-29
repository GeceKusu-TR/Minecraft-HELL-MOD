package com.example.minecraftmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneTorchBlock.class)
public abstract class RedstoneTorchBlockMixin {
	@Inject(method = "isToggledTooFrequently", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$disableRedstoneTorchBurnout(Level level, BlockPos pos, boolean toggleOff, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
