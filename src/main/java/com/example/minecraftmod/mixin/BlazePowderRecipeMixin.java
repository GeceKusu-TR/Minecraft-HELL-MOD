package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NetherRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapelessRecipe.class)
public abstract class BlazePowderRecipeMixin {
	@Inject(
		method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
		at = @At("RETURN"),
		cancellable = true
	)
	private void minecraftmod$limitBlazePowderCraftResult(CraftingInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack result = cir.getReturnValue();
		if (result.is(Items.BLAZE_POWDER) && result.getCount() > NetherRules.BLAZE_POWDER_PER_ROD) {
			ItemStack cappedResult = result.copy();
			cappedResult.setCount(NetherRules.BLAZE_POWDER_PER_ROD);
			cir.setReturnValue(cappedResult);
		}
	}
}
