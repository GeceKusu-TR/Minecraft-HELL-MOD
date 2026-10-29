package com.example.minecraftmod.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class DoorBlockWaterlogMixin {
	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void minecraftmod$preventUnderwaterDoorPocket(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
		BlockState state = cir.getReturnValue();
		if (state == null || !context.getLevel().getFluidState(context.getClickedPos()).is(FluidTags.WATER)) {
			return;
		}

		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			cir.setReturnValue(state.setValue(BlockStateProperties.WATERLOGGED, true));
		} else {
			cir.setReturnValue(null);
		}
	}
}
