package com.example.minecraftmod.mixin;

import com.example.minecraftmod.ReturnRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Villager.class)
public abstract class VillagerTradesMixin {
	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void minecraftmod$addReturnBookTrade(ServerLevel level, CallbackInfo ci) {
		Villager villager = (Villager)(Object)this;
		if (!villager.getVillagerData().profession().is(VillagerProfession.FARMER) || villager.getVillagerData().level() != 1) {
			return;
		}

		MerchantOffers offers = villager.getOffers();
		for (MerchantOffer offer : offers) {
			if (ReturnRules.isReturnBook(offer.getResult(), level.registryAccess())) {
				return;
			}
		}

		offers.add(new MerchantOffer(
			new ItemCost(Items.EMERALD, 64),
			Optional.of(new ItemCost(Items.EMERALD, 64)),
			ReturnRules.createReturnBook(level.registryAccess()),
			4,
			2,
			0.05F
		));
	}
}
