package com.example.minecraftmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceMeltMixin {
	@Inject(method = "melt", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$preventIceMelt(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
		ci.cancel();
	}
}
