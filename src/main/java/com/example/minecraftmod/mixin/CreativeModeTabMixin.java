package com.example.minecraftmod.mixin;

import com.example.minecraftmod.ReturnRules;
import java.util.Collection;
import java.util.Set;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {
	@Shadow
	private Collection<ItemStack> displayItems;

	@Shadow
	private Set<ItemStack> displayItemsSearchTab;

	@Inject(method = "buildContents", at = @At("TAIL"))
	private void minecraftmod$addReturnBookToCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
		if (displayItems == null) {
			return;
		}

		boolean tabContainsEnchantedBooks = displayItems.stream().anyMatch(stack -> stack.is(Items.ENCHANTED_BOOK));
		if (!tabContainsEnchantedBooks) {
			return;
		}

		ItemStack returnBook = ReturnRules.createReturnBook(parameters.holders());
		displayItems.add(returnBook);
		if (displayItemsSearchTab != null) {
			displayItemsSearchTab.add(returnBook.copy());
		}
	}
}
