package com.example.minecraftmod.mixin;

import com.example.minecraftmod.DirtRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class DirtMiningMixin {
	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockDirtWithoutShovel(BlockState blockState, CallbackInfoReturnable<Float> cir) {
		ItemStack mainHandItem = ((Player)(Object)this).getMainHandItem();
		if (DirtRules.isDirt(blockState) && !DirtRules.isShovel(mainHandItem)) {
			cir.setReturnValue(0.0F);
		}
	}
}
