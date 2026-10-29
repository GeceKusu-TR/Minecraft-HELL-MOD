package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GrassCurseRules;
import com.example.minecraftmod.AdvancementRules;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemGrassCurseMixin {
	@WrapMethod(method = "place")
	private InteractionResult minecraftmod$trackBlockPlacer(BlockPlaceContext context, Operation<InteractionResult> original) {
		return AdvancementRules.duringPlacement(context.getPlayer() instanceof ServerPlayer player ? player : null,
			() -> original.call(context));
	}

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockCursedGrassPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (GrassCurseRules.isCursedGrass(context.getItemInHand())) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
