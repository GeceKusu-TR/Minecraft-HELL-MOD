package com.example.minecraftmod;

import static net.minecraft.commands.Commands.literal;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.entity.EntityTypeTest;

public final class NoMobCommands {
	private static final double SPAWN_BLOCK_RADIUS = 128.0D;
	private static final double SPAWN_BLOCK_RADIUS_SQR = SPAWN_BLOCK_RADIUS * SPAWN_BLOCK_RADIUS;
	private static final Set<UUID> NO_MOB_PLAYERS = ConcurrentHashMap.newKeySet();

	private NoMobCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("nomob")
			.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
			.executes(context -> enableNoMob(context.getSource()))
			.then(literal("off")
				.executes(context -> disableNoMob(context.getSource()))));
	}

	public static boolean isNoMobEnabled(ServerPlayer player) {
		return NO_MOB_PLAYERS.contains(player.getUUID());
	}

	public static void clearPlayer(UUID playerId) {
		NO_MOB_PLAYERS.remove(playerId);
	}

	public static boolean shouldBlockHostileSpawn(ServerLevel level, BlockPos pos) {
		for (ServerPlayer player : level.players()) {
			if (isNoMobSpawnBlockedNear(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)) {
				return true;
			}
		}

		return false;
	}

	public static boolean shouldBlockHostileSpawn(ServerLevel level, Entity entity) {
		if (!(entity instanceof Enemy)) {
			return false;
		}

		for (ServerPlayer player : level.players()) {
			if (isNoMobSpawnBlockedNear(player, entity.getX(), entity.getY(), entity.getZ())) {
				return true;
			}
		}

		return false;
	}

	private static boolean isNoMobSpawnBlockedNear(ServerPlayer player, double x, double y, double z) {
		if (!isNoMobEnabled(player) || player.isCreative() || player.isSpectator()) {
			return false;
		}

		double dx = player.getX() - x;
		double dy = player.getY() - y;
		double dz = player.getZ() - z;
		return dx * dx + dy * dy + dz * dz <= SPAWN_BLOCK_RADIUS_SQR;
	}

	private static int enableNoMob(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		NO_MOB_PLAYERS.add(player.getUUID());
		int removedCount = removeMobsTargetingPlayer(player);
		source.sendSuccess(() -> Component.literal("NoMob acildi. " + removedCount + " saldirgan mob yok edildi."), true);
		return removedCount;
	}

	private static int disableNoMob(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		NO_MOB_PLAYERS.remove(player.getUUID());
		source.sendSuccess(() -> Component.literal("NoMob kapatildi."), true);
		return 1;
	}

	private static int removeMobsTargetingPlayer(ServerPlayer player) {
		ServerLevel level = (ServerLevel)player.level();
		List<? extends Mob> attackers = level.getEntities(
			EntityTypeTest.forClass(Mob.class),
			mob -> mob.isAlive() && mob.getTarget() == player
		);

		for (Mob attacker : attackers) {
			attacker.setTarget(null);
			attacker.discard();
		}

		return attackers.size();
	}
}
