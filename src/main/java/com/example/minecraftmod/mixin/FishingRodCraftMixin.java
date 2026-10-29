package com.example.minecraftmod.mixin;

import com.example.minecraftmod.FishingRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class FishingRodCraftMixin {
	@Inject(method = "onCraftedBy", at = @At("RETURN"))
	private void minecraftmod$curseCraftedFishingRods(Player player, int amount, CallbackInfo ci) {
		if (player.level().isClientSide()) {
			return;
		}

		FishingRules.enchantCraftedFishingRod((ServerLevel)player.level(), (ItemStack)(Object)this);
	}
}
