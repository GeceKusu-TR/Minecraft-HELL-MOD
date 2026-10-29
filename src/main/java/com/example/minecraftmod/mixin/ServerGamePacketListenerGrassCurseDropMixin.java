package com.example.minecraftmod.mixin;

import com.example.minecraftmod.GrassCurseRules;
import com.example.minecraftmod.AdvancementRules;
import com.example.minecraftmod.EndExperienceRules;
import com.example.minecraftmod.EndTermsRules;
import com.example.minecraftmod.NoMobCommands;
import com.example.minecraftmod.TestBookRules;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerGrassCurseDropMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
	private void minecraftmod$blockCursedGrassHotbarDrop(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
		ServerboundPlayerActionPacket.Action action = packet.getAction();
		if ((action == ServerboundPlayerActionPacket.Action.DROP_ITEM || action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)
			&& GrassCurseRules.isCursedGrass(this.player.getMainHandItem())) {
			syncInventory();
			ci.cancel();
		}
	}

	@Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
	private void minecraftmod$blockCursedGrassInventoryDrop(ServerboundContainerClickPacket packet, CallbackInfo ci) {
		if (packet.containerId() != this.player.containerMenu.containerId) {
			return;
		}
		if (packet.slotNum() == -999 && packet.clickType() == ClickType.PICKUP
			&& GrassCurseRules.isCursedGrass(this.player.containerMenu.getCarried())) {
			syncInventory();
			ci.cancel();
			return;
		}
		if (packet.clickType() != ClickType.THROW) {
			return;
		}

		int slot = packet.slotNum();
		if (slot < 0 || slot >= this.player.containerMenu.slots.size()) {
			return;
		}

		ItemStack stack = this.player.containerMenu.getSlot(slot).getItem();
		if (GrassCurseRules.isCursedGrass(stack)) {
			syncInventory();
			ci.cancel();
		}
	}

	private void syncInventory() {
		this.player.containerMenu.sendAllDataToRemote();
	}

	@Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
	private void minecraftmod$keepCreativeCursedGrass(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
		int slot = packet.slotNum();
		if ((slot < 0 && GrassCurseRules.isCursedGrass(packet.itemStack()))
			|| (slot >= 0 && slot < player.inventoryMenu.slots.size()
				&& GrassCurseRules.isCursedGrass(player.inventoryMenu.getSlot(slot).getItem()))) {
			player.inventoryMenu.sendAllDataToRemote();
			ci.cancel();
		}
	}

	@Inject(method = "onDisconnect", at = @At("HEAD"))
	private void minecraftmod$clearPlayerStateOnDisconnect(DisconnectionDetails details, CallbackInfo ci) {
		GrassCurseRules.onDisconnect(this.player);
		AdvancementRules.clearPlayer(this.player.getUUID());
		NoMobCommands.clearPlayer(this.player.getUUID());
		EndExperienceRules.clearPlayer(this.player);
		EndTermsRules.onDisconnect(this.player);
		TestBookRules.onDisconnect(this.player);
	}
}
