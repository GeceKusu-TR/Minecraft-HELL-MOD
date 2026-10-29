package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AdvancementRules;
import com.example.minecraftmod.DirtRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class DirtToolBreakMixin {
	@Inject(method = "mineBlock", at = @At("TAIL"))
	private void minecraftmod$breakShovelAfterOneDirt(Level level, BlockState blockState, BlockPos blockPos, Player player, CallbackInfo ci) {
		ItemStack stack = (ItemStack)(Object)this;
		if (level.isClientSide() || !DirtRules.isDirt(blockState) || !DirtRules.isShovel(stack)) {
			return;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			AdvancementRules.grantDirtShovel(serverPlayer);
		}

		EquipmentSlot slot = player.getMainHandItem() == stack ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
		stack.hurtAndBreak(stack.getMaxDamage(), player, slot);
	}
}
