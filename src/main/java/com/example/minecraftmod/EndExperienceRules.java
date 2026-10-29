package com.example.minecraftmod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

public final class EndExperienceRules {
	private static final Map<UUID, PendingTeleport> PENDING_TELEPORTS = new ConcurrentHashMap<>();
	private static final Set<UUID> TELEPORTING_NOW = ConcurrentHashMap.newKeySet();
	private static final String[] LOADING_MESSAGES = {
		"The End y\u00fckleniyor...",
		"Shader'lar derleniyor...",
		"Enderman'lar haz\u0131rlan\u0131yor...",
		"Ejderha uyand\u0131r\u0131l\u0131yor...",
		"Gereksiz bekleme ekleniyor..."
	};

	private EndExperienceRules() {
	}

	public static boolean shouldDelayEndTeleport(ServerPlayer player, TeleportTransition transition) {
		if (transition.newLevel().dimension() != Level.END || TELEPORTING_NOW.remove(player.getUUID())) {
			return false;
		}

		if (PENDING_TELEPORTS.containsKey(player.getUUID())) {
			return true;
		}

		PENDING_TELEPORTS.put(player.getUUID(), new PendingTeleport(
			transition,
			player.level().dimension(),
			player.getX(),
			player.getY(),
			player.getZ(),
			player.getYRot(),
			player.getXRot()
		));
		sendTitle(player, LOADING_MESSAGES[0]);
		return true;
	}

	public static void tick(ServerPlayer player) {
		PendingTeleport pending = PENDING_TELEPORTS.get(player.getUUID());
		if (pending != null) {
			if (!player.isAlive() || player.level().dimension() != pending.originDimension) {
				clearPlayer(player);
				return;
			}

			player.teleportTo(pending.x, pending.y, pending.z);
			player.setYRot(pending.yRot);
			player.setXRot(pending.xRot);
			player.setDeltaMovement(0.0D, 0.0D, 0.0D);
			player.hurtMarked = true;

			pending.age++;
			int index = Math.min(LOADING_MESSAGES.length - 1, pending.age / 35);
			if (index != pending.lastMessage) {
				pending.lastMessage = index;
				sendTitle(player, LOADING_MESSAGES[index]);
			}

			if (pending.age >= 35 * LOADING_MESSAGES.length) {
				PENDING_TELEPORTS.remove(player.getUUID());
				TELEPORTING_NOW.add(player.getUUID());
				player.teleport(pending.transition);
				player.connection.send(new ClientboundClearTitlesPacket(false));
			}
			return;
		}
	}

	public static void clearPlayer(ServerPlayer player) {
		PENDING_TELEPORTS.remove(player.getUUID());
		TELEPORTING_NOW.remove(player.getUUID());
		player.connection.send(new ClientboundClearTitlesPacket(false));
	}

	private static void sendTitle(ServerPlayer player, String text) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 5));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(text)));
	}

	private static final class PendingTeleport {
		private final TeleportTransition transition;
		private final net.minecraft.resources.ResourceKey<Level> originDimension;
		private final double x;
		private final double y;
		private final double z;
		private final float yRot;
		private final float xRot;
		private int age;
		private int lastMessage;

		private PendingTeleport(TeleportTransition transition, net.minecraft.resources.ResourceKey<Level> originDimension, double x, double y, double z, float yRot, float xRot) {
			this.transition = transition;
			this.originDimension = originDimension;
			this.x = x;
			this.y = y;
			this.z = z;
			this.yRot = yRot;
			this.xRot = xRot;
		}
	}
}
