package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SpearRules;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentHelper.class)
public abstract class SpearLungeMixin {
	@WrapMethod(method = "doLungeEffects")
	private static void minecraftmod$trackLungeCost(ServerLevel serverLevel, Entity entity, Operation<Void> original) {
		boolean openedScope = false;
		if (entity instanceof Player player) {
			HolderGetter<Enchantment> enchantments = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			ItemStack mainHandItem = player.getMainHandItem();
			if (SpearRules.isSpear(mainHandItem)
					&& EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.LUNGE), mainHandItem) > 0) {
				SpearRules.beginLunge(player);
				openedScope = true;
			}
		}

		try {
			original.call(serverLevel, entity);
		} finally {
			if (openedScope) {
				SpearRules.endLunge();
			}
		}
	}
}
