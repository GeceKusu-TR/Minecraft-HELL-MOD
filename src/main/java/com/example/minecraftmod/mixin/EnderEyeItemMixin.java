package com.example.minecraftmod.mixin;

import com.example.minecraftmod.EndPortalRitualRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$startEndPortalRitualInsteadOfOpening(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
			return;
		}

		var clickedState = level.getBlockState(context.getClickedPos());
		if (clickedState.is(Blocks.END_PORTAL_FRAME)
			&& clickedState.getValue(EndPortalFrameBlock.HAS_EYE)
			&& EndPortalRitualRules.startFromFrame(level, context.getClickedPos(), player)) {
			cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
			return;
		}

		if (!clickedState.is(Blocks.END_PORTAL_FRAME)
			|| clickedState.getValue(EndPortalFrameBlock.HAS_EYE)
			|| !EndPortalRitualRules.shouldHijackTwelfthEye(level, context.getClickedPos())) {
			return;
		}

		context.getItemInHand().shrink(1);
		EndPortalRitualRules.placeTwelfthEyeAndStart(level, context.getClickedPos(), player);
		cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
	}
}
