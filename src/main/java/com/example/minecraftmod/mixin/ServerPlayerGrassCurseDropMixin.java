package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GrassCurseRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerGrassCurseDropMixin {
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$blockCursedGrassDrop(ItemStack stack, boolean dropAround, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
		if (GrassCurseRules.isCursedGrass(stack)) {
			GrassCurseRules.retainDroppedGrass((ServerPlayer)(Object)this, stack);
			cir.setReturnValue(null);
		}
	}
}
