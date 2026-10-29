package com.example.minecraftmod.mixin;

import com.example.minecraftmod.LeatherHelmetRules;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class LeatherHelmetEnchantmentMixin {
	@Inject(method = "enchant", at = @At("TAIL"))
	private void minecraftmod$stripForbiddenLeatherHelmetEnchantments(Holder<Enchantment> enchantment, int level, CallbackInfo ci) {
		ItemStack stack = (ItemStack)(Object)this;
		if (!LeatherHelmetRules.isLeatherArmor(stack) || (!enchantment.is(Enchantments.UNBREAKING) && !enchantment.is(Enchantments.MENDING))) {
			return;
		}

		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(holder -> holder.is(Enchantments.UNBREAKING) || holder.is(Enchantments.MENDING)));
	}
}
