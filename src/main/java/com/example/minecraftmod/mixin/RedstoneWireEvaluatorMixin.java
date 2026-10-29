package com.example.minecraftmod.mixin;

import com.example.minecraftmod.RedstoneRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultRedstoneWireEvaluator.class)
public abstract class RedstoneWireEvaluatorMixin {
	@Inject(method = "calculateTargetStrength", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$calculateStableNoFalloffSignal(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(RedstoneRules.stableWireSignal(level, pos));
	}
}
