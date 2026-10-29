package com.example.minecraftmod.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
	@Accessor("inputSlots")
	Container minecraftmod$getInputSlots();

	@Accessor("resultSlots")
	ResultContainer minecraftmod$getResultSlots();

	@Accessor("player")
	Player minecraftmod$getPlayer();
}
