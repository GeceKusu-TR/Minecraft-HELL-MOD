package com.example.minecraftmod;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.animal.golem.SnowGolem;

public final class DeathMessageRules {
	// v3 fragment: "Zindanların içinde moblar, tuzaklar ve geliştiricinin kötü kararları bulunacak."
	private static final Map<String, List<String>> MESSAGES = loadMessages();

	private DeathMessageRules() {
	}

	public static void showRandomMessage(ServerPlayer player, DamageSource damageSource) {
		if (!(player.level() instanceof ServerLevel)) {
			return;
		}

		List<String> categoryMessages = MESSAGES.get(categoryFor(damageSource));
		if (categoryMessages == null || categoryMessages.isEmpty()) {
			categoryMessages = MESSAGES.get("Bilinmeyen");
		}
		if (categoryMessages == null || categoryMessages.isEmpty()) {
			return;
		}

		String message = categoryMessages.get(player.getRandom().nextInt(categoryMessages.size()));
		String elapsed = formatGameTime(player.tickCount);
		message = message.replace("[OYUN İÇİ SÜRE]", elapsed);
		if (!message.contains(elapsed)) {
			message = "Oyunda " + elapsed + " dayanabildin. " + message;
		}
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
	}

	private static String formatGameTime(long gameTimeTicks) {
		long totalSeconds = Math.max(0L, gameTimeTicks) / 20L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0L) {
			return hours + " saat " + minutes + " dakika " + seconds + " saniye";
		}
		return minutes + " dakika " + seconds + " saniye";
	}

	private static String categoryFor(DamageSource damageSource) {
		if (damageSource.is(DamageTypes.CACTUS)) return "Modlu Kaktüs";
		if (damageSource.is(DamageTypes.LAVA)) return "Lav";
		if (damageSource.is(DamageTypes.LIGHTNING_BOLT)) {
			Entity source = damageSource.getEntity();
			return source instanceof LightningBolt bolt && bolt.getTags().contains("minecraftmod_ultra_thunder")
				? "Ultra Yıldırım Dalgası" : "Yıldırım";
		}
		if (damageSource.is(DamageTypes.FALL)) return "Düşme";
		if (damageSource.is(DamageTypes.DROWN)) return "Boğulma";
		if (damageSource.is(DamageTypeTags.IS_EXPLOSION)) return "TNT ve Patlama";
		if (damageSource.is(DamageTypes.IN_WALL)) return "Sıkışma";
		if (damageSource.is(DamageTypes.FLY_INTO_WALL)) return "Düşme";
		if (damageSource.is(DamageTypes.MAGIC) || damageSource.is(DamageTypes.INDIRECT_MAGIC)) return "Zehir";
		if (damageSource.is(DamageTypes.FREEZE)) return "Oksijensizlik";

		Entity source = damageSource.getEntity();
		if (source instanceof SnowGolem) return "Kardan Golem";
		if (source instanceof AbstractSkeleton skeleton && skeleton.getTags().stream().anyMatch(tag -> tag.equals("minecraftmod_skeleton_leader"))) {
			return "Lider İskelet";
		}
		if (source instanceof Spider) return "Örümcek";
		if (source instanceof Phantom) return "Phantom";
		if (source instanceof Zombie) return "Zombi";
		if (source instanceof Creeper) return "Patlama";
		if (source instanceof net.minecraft.world.entity.monster.Monster) return "Genel Mob";
		if (damageSource.is(DamageTypes.STARVE)) return "Açlık";
		if (FireRules.isFireDamage(damageSource)) return "Ateş";
		return "Bilinmeyen";
	}

	private static Map<String, List<String>> loadMessages() {
		try (InputStream stream = DeathMessageRules.class.getResourceAsStream("/minecraftmod/death_messages.txt")) {
			if (stream == null) {
				MinecraftMod.LOGGER.error("Death message resource is missing.");
				return Map.of();
			}

		Map<String, List<String>> messages = new HashMap<>();
		String[] currentCategory = {"Bilinmeyen"};
		new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
				.lines()
				.map(String::trim)
				.forEach(line -> {
					if (line.startsWith("===") && line.endsWith("===")) {
						currentCategory[0] = line.substring(3, line.length() - 3).trim();
					} else if (!line.isEmpty() && !line.startsWith("#")) {
						messages.computeIfAbsent(currentCategory[0], ignored -> new ArrayList<>()).add(line);
					}
				});
			return messages.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
		} catch (IOException exception) {
			MinecraftMod.LOGGER.error("Could not load death messages.", exception);
			return Map.of();
		}
	}
}
