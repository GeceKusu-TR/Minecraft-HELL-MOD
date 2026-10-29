package com.example.minecraftmod.mixin;

import com.example.minecraftmod.FishingRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookLootMixin {
	@Shadow
	private int nibble;

	@Shadow
	private int timeUntilLured;

	@Shadow
	private int timeUntilHooked;

	@Shadow
	public abstract Player getPlayerOwner();

	@WrapOperation(
		method = "retrieve",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;")
	)
	private ObjectArrayList<ItemStack> minecraftmod$forceWorstFishingLoot(
		LootTable lootTable,
		LootParams params,
		Operation<ObjectArrayList<ItemStack>> original,
		ItemStack rod
	) {
		if (((FishingHook)(Object)this).level() instanceof ServerLevel level && FishingRules.isUnluckyFishingRod(level, rod)) {
			if (getPlayerOwner() instanceof ServerPlayer player) {
				FishingRules.maybeThrowFishingTrident(level, player, ((FishingHook)(Object)this).getRandom());
			}
			return FishingRules.worstFishingLoot(((FishingHook)(Object)this).getRandom());
		}

		return original.call(lootTable, params);
	}

	@Inject(method = "catchingFish", at = @At("TAIL"))
	private void minecraftmod$makeUnluckyFishingFast(net.minecraft.core.BlockPos pos, CallbackInfo ci) {
		if (!(((FishingHook)(Object)this).level() instanceof ServerLevel level)) {
			return;
		}

		Player player = getPlayerOwner();
		if (player == null || !FishingRules.hasUnluckyFishingRod(level, player)) {
			return;
		}

		if (this.nibble > 0) {
			return;
		}
		if (this.timeUntilHooked > 1) {
			this.timeUntilHooked = 1;
		}
		if (this.timeUntilHooked <= 0 && this.timeUntilLured > FishingRules.FAST_FISHING_WAIT_TICKS) {
			this.timeUntilLured = FishingRules.FAST_FISHING_WAIT_TICKS;
		}
	}
}
