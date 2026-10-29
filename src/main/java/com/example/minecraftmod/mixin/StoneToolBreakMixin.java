package com.example.minecraftmod.mixin;

import com.example.minecraftmod.StoneRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class StoneToolBreakMixin {
	@Inject(method = "mineBlock", at = @At("TAIL"))
	private void minecraftmod$applyStoneWear(Level level, BlockState blockState, BlockPos blockPos, Player player, CallbackInfo ci) {
		ItemStack stack = (ItemStack)(Object)this;
		if (level.isClientSide() || !StoneRules.isPickaxe(stack)) {
			return;
		}

		int extraDamage = StoneRules.getAdditionalDurabilityDamage(blockState, stack);
		if (extraDamage <= 0) {
			return;
		}

		EquipmentSlot slot = player.getMainHandItem() == stack ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
		stack.hurtAndBreak(extraDamage, player, slot);
	}
}
