package com.example.minecraftmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerBareHandAttackMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void minecraftmod$hurtBareHands(Entity target, CallbackInfo ci) {
		Player player = (Player)(Object)this;
		if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide()) {
			return;
		}

		if (serverPlayer.isCreative() || !serverPlayer.getMainHandItem().isEmpty()) {
			return;
		}

		ServerLevel serverLevel = (ServerLevel)serverPlayer.level();
		serverPlayer.hurtServer(serverLevel, serverPlayer.damageSources().generic(), 1.0F);
	}
}
