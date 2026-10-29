package com.example.minecraftmod.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSleepColdMixin {
	@Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
	private void minecraftmod$requireLeatherBootsInSnowyBiome(BlockPos bedPos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		ServerPlayer player = (ServerPlayer)(Object)this;
		if (!player.level().getBiome(bedPos).value().coldEnoughToSnow(bedPos, player.level().getSeaLevel())
			|| player.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS)) {
			return;
		}

		Component message = Component.literal("Uyumak i\u00e7in \u00e7ok so\u011fuk.");
		player.displayClientMessage(message, true);
		cir.setReturnValue(Either.left(new Player.BedSleepingProblem(message)));
	}
}
