package com.example.minecraftmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow
	private ServerLevel level;

	@Shadow
	@Final
	private ServerPlayer player;

	@Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
	private void minecraftmod$hurtBareHandsOnBlockBreak(
		BlockPos pos,
		ServerboundPlayerActionPacket.Action action,
		Direction direction,
		int worldHeight,
		int sequence,
		CallbackInfo ci
	) {
		if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
			return;
		}

		if (player.isCreative()) {
			return;
		}

		ItemStack mainHand = player.getMainHandItem();
		if (!mainHand.isEmpty()) {
			return;
		}

		if (!level.isLoaded(pos) || level.getBlockState(pos).isAir()) {
			return;
		}

		if (!player.isWithinBlockInteractionRange(pos, 1.0D)) {
			return;
		}

		player.hurtServer(level, player.damageSources().generic(), 1.0F);
	}
}
