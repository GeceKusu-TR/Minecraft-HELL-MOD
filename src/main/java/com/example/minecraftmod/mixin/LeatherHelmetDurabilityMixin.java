package com.example.minecraftmod.mixin;

import com.example.minecraftmod.LeatherHelmetRules;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class LeatherHelmetDurabilityMixin {
	@Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$lowerLeatherHelmetDurability(CallbackInfoReturnable<Integer> cir) {
		ItemStack stack = (ItemStack)(Object)this;
		if (LeatherHelmetRules.isLeatherArmor(stack)) {
			cir.setReturnValue(LeatherHelmetRules.MAX_DAMAGE);
		}
	}
}
