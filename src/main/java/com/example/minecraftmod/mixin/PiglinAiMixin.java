package com.example.minecraftmod.mixin;

import com.example.minecraftmod.PiglinRules;
import com.example.minecraftmod.NOSpeedrunRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
	@Inject(method = "mobInteract", at = @At("RETURN"))
	private static void minecraftmod$rememberDirectBarterer(ServerLevel level, Piglin piglin, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue() instanceof InteractionResult.Success) {
			PiglinRules.rememberBarterer(piglin, player);
		}
	}

	@Inject(method = "pickUpItem", at = @At("HEAD"))
	private static void minecraftmod$rememberDroppedBarterer(ServerLevel level, Piglin piglin, ItemEntity itemEntity, CallbackInfo ci) {
		if (PiglinRules.isBarterCurrency(itemEntity.getItem())) {
			Entity owner = itemEntity.getOwner();
			PiglinRules.rememberBarterer(piglin, owner);
		}
	}

	@Inject(method = "getBarterResponseItems", at = @At("HEAD"), cancellable = true)
	private static void minecraftmod$mostlyGiveRootedDirt(Piglin piglin, CallbackInfoReturnable<List<ItemStack>> cir) {
		if (PiglinRules.shouldGiveRootedDirt(piglin)) {
			PiglinRules.grantPoopBarterAdvancement(piglin);
			cir.setReturnValue(PiglinRules.rootedDirtBarterResult());
		}
	}

	@Inject(method = "getBarterResponseItems", at = @At("RETURN"), cancellable = true)
	private static void minecraftmod$removeEnderPearlBarter(Piglin piglin, CallbackInfoReturnable<List<ItemStack>> cir) {
		PiglinRules.clearBarterer(piglin);
		cir.setReturnValue(NOSpeedrunRules.removeEnderPearlsFromBarter(cir.getReturnValue()));
	}
}
