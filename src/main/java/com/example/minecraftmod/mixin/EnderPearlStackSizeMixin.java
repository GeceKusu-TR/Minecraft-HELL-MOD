package com.example.minecraftmod.mixin;

import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Items.class)
public abstract class EnderPearlStackSizeMixin {
	@ModifyConstant(
		method = "<clinit>",
		constant = @Constant(intValue = 16),
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=ender_pearl"),
			to = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;ENDER_PEARL:Lnet/minecraft/world/item/Item;")
		)
	)
	private static int minecraftmod$increaseEnderPearlStackSize(int original) {
		return 64;
	}
}
