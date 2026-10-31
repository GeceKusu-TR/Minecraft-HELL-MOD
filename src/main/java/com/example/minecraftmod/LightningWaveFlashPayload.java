package com.example.minecraftmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells a modded client to show the lightning-wave warning flash. */
public record LightningWaveFlashPayload(int durationTicks) implements CustomPacketPayload {
	public static final Type<LightningWaveFlashPayload> TYPE = new Type<>(MinecraftMod.id("lightning_wave_flash"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LightningWaveFlashPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		LightningWaveFlashPayload::durationTicks,
		LightningWaveFlashPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
