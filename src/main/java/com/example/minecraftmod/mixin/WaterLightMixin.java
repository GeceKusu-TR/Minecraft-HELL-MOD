package com.example.minecraftmod.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class WaterLightMixin {
	@Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$makeWaterBrightLikeNetherLava(CallbackInfoReturnable<Integer> cir) {
		BlockState state = (BlockState)(Object)this;
		if (state.is(Blocks.WATER)) {
			cir.setReturnValue(15);
		}
	}
}
