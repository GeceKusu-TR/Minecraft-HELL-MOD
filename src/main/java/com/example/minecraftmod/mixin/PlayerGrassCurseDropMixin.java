package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GrassCurseRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerGrassCurseDropMixin {
	@Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockCursedGrassCreativeDrop(ItemStack stack, CallbackInfo ci) {
		if (GrassCurseRules.isCursedGrass(stack)) {
			ci.cancel();
		}
	}
}
