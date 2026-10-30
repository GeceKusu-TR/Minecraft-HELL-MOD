package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class EndCompletionRules {
	private static final String COMPLETED_TAG = "minecraftmod_end_completed";
	private static final int PORTAL_SEARCH_RADIUS = 4;
	private static final int PORTAL_VERTICAL_RADIUS = 2;

	private EndCompletionRules() {
	}

	public static boolean isCompleted(ServerPlayer player) {
		return player.getTags().contains(COMPLETED_TAG);
	}

	public static boolean isHardcoreEscalated(ServerPlayer player) {
		return player.level().getLevelData().isHardcore()
			&& player.getTags().contains(DifficultyScalingRules.HARDCORE_ESCALATED_TAG);
	}

	public static void onEndReturn(ServerPlayer player) {
		onEndReturn(player, null, null);
	}

	public static void onEndReturn(ServerPlayer player, ServerLevel sourceLevel, BlockPos sourcePosition) {
		if (!(player.level() instanceof ServerLevel level)
			|| level.dimension() != Level.OVERWORLD
			|| player.getTags().contains(COMPLETED_TAG)) {
			return;
		}

		player.addTag(COMPLETED_TAG);
		String elapsed = formatGameTime(level.getGameTime());
		if (level.getLevelData().isHardcore()) {
			player.addTag(DifficultyScalingRules.HARDCORE_ESCALATED_TAG);
			DifficultyScalingRules.activateHardcoreEscalation();
			send(player, "Tamam.. oyunu 2 kat zorlaştırıyorum yiyorsa bu sefer tekrar bitir.");
			send(player, "LANET OLSUN! Oyunu " + elapsed + " sürede bitirdin.");
			send(player, "Oyun dışı olan süreyi MAALESEF GÖREMİYORUM O KADARDA DEĞİL!");
			send(player, "Herneyse.. Lütfen oyunu artık kapat. Çimene dokun. Lütfen..");
		} else {
			send(player, "Neden? nasıl? bunu nasıl başardın? bu imkansızdı..");
			send(player, "Herneyse oyunu " + elapsed + " sürede bitirdin.");
			send(player, "Oyun dışı olan süreyi MAALESEF GÖREMİYORUM O KADARDA DEĞİL!");
			send(player, "Herneyse.. bakalım bu sefer bu modda oyunu bitirebilecekmisin? Kendine olan güvenin varsa şuan oyunu kapat!");
		}
		ServerPlayNetworking.send(player, new CompletionHudPayload(true, isHardcoreEscalated(player)));

		if (sourceLevel != null && sourcePosition != null) {
			closeNearbyEndPortal(sourceLevel, sourcePosition);
		}
	}

	private static void send(ServerPlayer player, String message) {
		player.sendSystemMessage(
			Component.literal("<Oyun Mod Yapımcısı> ").withStyle(ChatFormatting.RED)
				.append(Component.literal(message))
		);
	}

	private static String formatGameTime(long gameTimeTicks) {
		long totalSeconds = Math.max(0L, gameTimeTicks) / 20L;
		long days = totalSeconds / 86_400L;
		long hours = (totalSeconds % 86_400L) / 3_600L;
		long minutes = (totalSeconds % 3_600L) / 60L;
		long seconds = totalSeconds % 60L;

		StringBuilder result = new StringBuilder();
		if (days > 0L) {
			result.append(days).append(" gün ");
		}
		if (hours > 0L || days > 0L) {
			result.append(hours).append(" saat ");
		}
		if (minutes > 0L || hours > 0L || days > 0L) {
			result.append(minutes).append(" dakika ");
		}
		result.append(seconds).append(" saniye");
		return result.toString();
	}

	private static void closeNearbyEndPortal(ServerLevel level, BlockPos center) {
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		for (int y = -PORTAL_VERTICAL_RADIUS; y <= PORTAL_VERTICAL_RADIUS; y++) {
			for (int x = -PORTAL_SEARCH_RADIUS; x <= PORTAL_SEARCH_RADIUS; x++) {
				for (int z = -PORTAL_SEARCH_RADIUS; z <= PORTAL_SEARCH_RADIUS; z++) {
					mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
					if (level.getBlockState(mutable).is(Blocks.END_PORTAL)) {
						level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
					}
				}
			}
		}
	}
}
