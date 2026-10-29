package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NetherWaterRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionNetherWaterMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$generateNetherWaterInsteadOfLava(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		WorldGenRegion region = (WorldGenRegion)(Object)this;
		if (NetherWaterRules.isNether(level) && NetherWaterRules.isLava(state)) {
			cir.setReturnValue(region.setBlock(pos, NetherWaterRules.netherWater(), flags, maxUpdateDepth));
		}
	}
}
