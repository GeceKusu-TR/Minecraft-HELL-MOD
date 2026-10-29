package com.example.minecraftmod.mixin;

import com.example.minecraftmod.EndPortalRitualRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndPortalFrameBlock.class)
public abstract class EndPortalFrameBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$restartCompletedPortalRitual(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer && EndPortalRitualRules.startFromFrame(serverLevel, pos, serverPlayer)) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
		}
	}
}
