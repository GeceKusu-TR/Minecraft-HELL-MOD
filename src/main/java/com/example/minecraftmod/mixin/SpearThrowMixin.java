package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SpearRules;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class SpearThrowMixin {
	@Inject(method = "onCraftedBy", at = @At("RETURN"))
	private void minecraftmod$addLungeToCraftedSpears(Player player, int amount, CallbackInfo ci) {
		if (player.level().isClientSide()) {
			return;
		}

		ItemStack stack = (ItemStack) (Object) this;
		if (!SpearRules.isSpear(stack)) {
			return;
		}

		ServerLevel serverLevel = (ServerLevel) player.level();
		HolderGetter<Enchantment> enchantments = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		stack.enchant(enchantments.getOrThrow(Enchantments.LUNGE), 3);
	}
}
