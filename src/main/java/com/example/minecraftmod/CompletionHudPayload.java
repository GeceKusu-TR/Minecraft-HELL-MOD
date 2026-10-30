package com.example.minecraftmod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CompletionHudPayload(boolean completed, boolean hardcoreEscalated) implements CustomPacketPayload {
	public static final Type<CompletionHudPayload> TYPE = new Type<>(MinecraftMod.id("completion_hud"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CompletionHudPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL,
		CompletionHudPayload::completed,
		ByteBufCodecs.BOOL,
		CompletionHudPayload::hardcoreEscalated,
		CompletionHudPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
