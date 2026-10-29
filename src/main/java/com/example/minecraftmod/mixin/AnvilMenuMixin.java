package com.example.minecraftmod.mixin;

import com.example.minecraftmod.LeatherHelmetRules;
import com.example.minecraftmod.ReturnRules;
import com.example.minecraftmod.WorldAnnoyanceRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private int repairItemCountCost;

	@Inject(method = "createResult", at = @At("TAIL"))
	private void minecraftmod$createReturnStickResult(CallbackInfo ci) {
		ItemCombinerMenuAccessor itemCombinerMenu = (ItemCombinerMenuAccessor) this;
		var inputSlots = itemCombinerMenu.minecraftmod$getInputSlots();
		ItemStack base = inputSlots.getItem(0);
		ItemStack addition = inputSlots.getItem(1);
		var holders = itemCombinerMenu.minecraftmod$getPlayer().level().registryAccess();
		if (!base.is(Items.STICK) || !ReturnRules.isReturnBook(addition, holders)) {
			minecraftmod$removeForbiddenLeatherHelmetResult();
			return;
		}

		ItemStack result = base.copyWithCount(1);
		ReturnRules.markReturnStick(result, holders);
		itemCombinerMenu.minecraftmod$getResultSlots().setItem(0, result);
		cost.set(1);
		repairItemCountCost = 1;
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void minecraftmod$capExpensiveAnvilCostForTax(CallbackInfo ci) {
		ItemCombinerMenuAccessor itemCombinerMenu = (ItemCombinerMenuAccessor) this;
		if (!itemCombinerMenu.minecraftmod$getResultSlots().getItem(0).isEmpty() && cost.get() > 39) {
			cost.set(39);
		}
	}

	private void minecraftmod$removeForbiddenLeatherHelmetResult() {
		ItemCombinerMenuAccessor itemCombinerMenu = (ItemCombinerMenuAccessor) this;
		ItemStack result = itemCombinerMenu.minecraftmod$getResultSlots().getItem(0);
		if (LeatherHelmetRules.hasForbiddenEnchantments(result, itemCombinerMenu.minecraftmod$getPlayer().level().registryAccess())) {
			itemCombinerMenu.minecraftmod$getResultSlots().setItem(0, ItemStack.EMPTY);
			cost.set(0);
			repairItemCountCost = 0;
		}
	}

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$allowReturnStickPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
		var inputSlots = ((ItemCombinerMenuAccessor) this).minecraftmod$getInputSlots();
		ItemStack base = inputSlots.getItem(0);
		ItemStack addition = inputSlots.getItem(1);
		if (base.is(Items.STICK) && ReturnRules.isReturnBook(addition, player.level().registryAccess())) {
			cir.setReturnValue(player.isCreative() || player.experienceLevel >= cost.get());
			return;
		}

		if (cost.get() >= 39) {
			cir.setReturnValue(player.isCreative() || player.experienceLevel >= cost.get() && WorldAnnoyanceRules.hasAnvilTax(player));
		}
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void minecraftmod$takeExpensiveAnvilTax(Player player, ItemStack stack, CallbackInfo ci) {
		if (!player.isCreative() && cost.get() >= 39) {
			WorldAnnoyanceRules.takeAnvilTax(player);
		}
	}
}
