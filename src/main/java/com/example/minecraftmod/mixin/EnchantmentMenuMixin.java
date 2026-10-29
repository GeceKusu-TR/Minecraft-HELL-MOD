package com.example.minecraftmod.mixin;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Unique
	private final Set<Integer> minecraftmod$repricedSlots = new HashSet<>();

	@Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$bargainForLevelThirty(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
		EnchantmentMenu menu = (EnchantmentMenu)(Object)this;
		if (id >= 0 && id < menu.costs.length && menu.costs[id] == 30 && minecraftmod$repricedSlots.add(id)) {
			menu.costs[id] = 31;
			player.displayClientMessage(Component.literal("Yo\u011fun talep nedeniyle fiyat g\u00fcncellendi: 31"), false);
			cir.setReturnValue(false);
		}
	}
}
