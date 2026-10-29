package com.example.minecraftmod.mixin;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
	@Unique
	private final Set<Integer> minecraftmod$repricedOffers = new HashSet<>();

	@Inject(method = "setSelectionHint", at = @At("TAIL"))
	private void minecraftmod$repriceTradeOnce(int selectionHint, CallbackInfo ci) {
		MerchantOffers offers = ((MerchantMenu)(Object)this).getOffers();
		if (selectionHint < 0 || selectionHint >= offers.size() || !minecraftmod$repricedOffers.add(selectionHint)) {
			return;
		}

		if (((MerchantMenuAccessor)this).minecraftmod$getTrader().isClientSide()) {
			return;
		}

		MerchantOffer original = offers.get(selectionHint);
		if (original.isOutOfStock() || Math.random() >= 0.65D) {
			return;
		}

		int oldCost = Math.max(1, original.getCostA().getCount());
		int increase = Math.min(64 - oldCost, 1 + (int)(Math.random() * 8.0D));
		if (increase > 0) {
			original.addToSpecialPriceDiff(increase);
		}
	}
}
