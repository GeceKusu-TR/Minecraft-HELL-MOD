package com.example.minecraftmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeathMessagePayload(String message) implements CustomPacketPayload {
	public static final Type<DeathMessagePayload> TYPE = new Type<>(MinecraftMod.id("death_message"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DeathMessagePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, DeathMessagePayload::message, DeathMessagePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
