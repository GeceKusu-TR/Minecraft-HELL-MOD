package com.example.minecraftmod;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.example.minecraftmod.mixin.PlayerInfoUpdatePacketAccessor;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class FakePlayerRules {
	private static final String BLACK_SKIN_TEXTURE_URL = "http://textures.minecraft.net/texture/c7acd6b5a7a61bb25892225d160138b4e97fa02cc33fbecb559deae4cb485f";
	private static final Map<UUID, Map<UUID, Integer>> TEMP_FAKE_PLAYERS = new ConcurrentHashMap<>();

	private FakePlayerRules() {
	}

	public static void tick(ServerPlayer player) {
		Map<UUID, Integer> active = TEMP_FAKE_PLAYERS.get(player.getUUID());
		if (active != null) {
			active.entrySet().removeIf(entry -> {
				int remaining = entry.getValue() - 1;
				if (remaining <= 0) {
					removeFromTab(player, entry.getKey());
					return true;
				}
				entry.setValue(remaining);
				return false;
			});
			if (active.isEmpty()) {
				TEMP_FAKE_PLAYERS.remove(player.getUUID());
			}
		}

		if (player.getRandom().nextInt(2400) == 0) {
			UUID uuid = UUID.nameUUIDFromBytes(("minecraftmod:blink:" + player.getUUID() + ":" + player.tickCount).getBytes(StandardCharsets.UTF_8));
			addToTab(player, uuid, "Player" + (10 + player.getRandom().nextInt(90)), true);
			TEMP_FAKE_PLAYERS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(uuid, 6);
		}
	}

	public static void addToTab(ServerPlayer player, UUID uuid, String name, boolean blackSkin) {
		Multimap<String, Property> rawProperties = HashMultimap.create();
		if (blackSkin) {
			rawProperties.put("textures", new Property("textures", blackSkinTextureValue()));
		}
		GameProfile fakeProfile = new GameProfile(uuid, name, new PropertyMap(rawProperties));
		ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
			uuid,
			fakeProfile,
			true,
			47,
			GameType.SURVIVAL,
			Component.literal(name),
			true,
			0,
			null
		);
		ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
			EnumSet.of(
				ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
				ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
			),
			List.of()
		);
		((PlayerInfoUpdatePacketAccessor)packet).minecraftmod$setEntries(List.of(entry));
		player.connection.send(packet);
	}

	public static void removeFromTab(ServerPlayer player, UUID uuid) {
		player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(uuid)));
	}

	private static String blackSkinTextureValue() {
		String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + BLACK_SKIN_TEXTURE_URL + "\",\"metadata\":{\"model\":\"classic\"}}}}";
		return Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));
	}
}
