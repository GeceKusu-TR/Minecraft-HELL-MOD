package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SpearRules;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentHelper.class)
public abstract class SpearLungeMixin {
	@Inject(method = "doLungeEffects", at = @At("HEAD"))
	private static void minecraftmod$beginLungeCost(ServerLevel serverLevel, Entity entity, CallbackInfo ci) {
		if (!(entity instanceof Player player)) {
			return;
		}

		HolderGetter<Enchantment> enchantments = serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		ItemStack mainHandItem = player.getMainHandItem();
		if (!SpearRules.isSpear(mainHandItem) || EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.LUNGE), mainHandItem) <= 0) {
			return;
		}

		SpearRules.beginLunge(player);
	}

	@Inject(method = "doLungeEffects", at = @At("RETURN"))
	private static void minecraftmod$endLungeCost(ServerLevel serverLevel, Entity entity, CallbackInfo ci) {
		SpearRules.endLunge();
	}
}
