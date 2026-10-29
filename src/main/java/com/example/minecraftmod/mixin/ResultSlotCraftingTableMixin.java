package com.example.minecraftmod.mixin;

import com.example.minecraftmod.WorldAnnoyanceRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotCraftingTableMixin {
	@Inject(method = "onTake", at = @At("TAIL"))
	private void minecraftmod$damageNearbyCraftingTable(Player player, ItemStack stack, CallbackInfo ci) {
		if (player.containerMenu instanceof CraftingMenu craftingMenu) {
			((CraftingMenuAccessor)craftingMenu).minecraftmod$getAccess().execute((level, pos) ->
				WorldAnnoyanceRules.onCraftTaken(player, level, pos));
		}
	}
}
