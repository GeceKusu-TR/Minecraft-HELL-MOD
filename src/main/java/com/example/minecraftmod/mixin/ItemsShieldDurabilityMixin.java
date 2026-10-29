package com.example.minecraftmod.mixin;

import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Items.class)
public abstract class ItemsShieldDurabilityMixin {
	@ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 336))
	private static int minecraftmod$increaseShieldDurability(int original) {
		return 500;
	}
}
