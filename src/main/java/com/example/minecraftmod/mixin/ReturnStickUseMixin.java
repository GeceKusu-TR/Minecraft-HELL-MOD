package com.example.minecraftmod.mixin;

import com.example.minecraftmod.ReturnRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ReturnStickUseMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$pullBlockWithReturnStick(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		if (!ReturnRules.isReturnStick(stack, context.getLevel().registryAccess())) {
			return;
		}

		if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player) {
			BlockState pulledState = level.getBlockState(context.getClickedPos());
			boolean pulled = ReturnRules.pullBlockToInventory(level, player, context.getClickedPos());
			if (pulled && !player.isCreative()) {
				ReturnRules.damageReturnStickAfterUse(level, player, stack, context.getClickedPos(), pulledState);
			}
			cir.setReturnValue(pulled ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL);
			return;
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}
}
