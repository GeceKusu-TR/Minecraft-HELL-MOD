package com.example.minecraftmod.mixin;

import java.util.List;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoUpdatePacketAccessor {
	@Mutable
	@Accessor("entries")
	void minecraftmod$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
