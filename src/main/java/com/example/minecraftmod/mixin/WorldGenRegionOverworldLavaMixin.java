package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OverworldLavaRules;
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
public abstract class WorldGenRegionOverworldLavaMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$generateOverworldLavaInsteadOfWater(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		WorldGenRegion region = (WorldGenRegion)(Object)this;
		if (OverworldLavaRules.isOverworld(level) && OverworldLavaRules.containsWater(state)) {
			if (OverworldLavaRules.isWater(state)) {
				cir.setReturnValue(region.setBlock(pos, OverworldLavaRules.overworldLava(), flags, maxUpdateDepth));
				return;
			}

			if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
				&& Boolean.TRUE.equals(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED))) {
				cir.setReturnValue(region.setBlock(
					pos,
					state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false),
					flags,
					maxUpdateDepth
				));
				return;
			}

			cir.setReturnValue(region.setBlock(pos, OverworldLavaRules.overworldLava(), flags, maxUpdateDepth));
		}
	}
}
