package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GrassCurseRules;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Inventory.class)
public abstract class InventoryGrassCurseDeathMixin {
	@WrapMethod(method = "dropAll")
	private void minecraftmod$keepCursedGrassOnDeath(Operation<Void> original) {
		Inventory inventory = (Inventory)(Object)this;
		Map<Integer, ItemStack> retained = new HashMap<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (GrassCurseRules.isCursedGrass(stack)) {
				retained.put(slot, stack);
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
		try {
			original.call();
		} finally {
			retained.forEach(inventory::setItem);
		}
	}
}
