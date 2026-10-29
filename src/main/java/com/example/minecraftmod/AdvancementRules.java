package com.example.minecraftmod;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class AdvancementRules {
	private static final Identifier VANILLA_ENTER_NETHER_ID = Identifier.withDefaultNamespace("story/enter_the_nether");
	private static final Identifier VANILLA_SUMMON_WITHER_ID = Identifier.withDefaultNamespace("nether/summon_wither");
	public static final Identifier ROOT_ID = MinecraftMod.id("root");
	public static final Identifier WELCOME_TO_HELL_ID = MinecraftMod.id("welcome_to_hell");
	public static final Identifier GHAST_DEATH_ID = MinecraftMod.id("ghast_death");
	public static final Identifier DIRT_SHOVEL_ID = MinecraftMod.id("dirt_shovel");
	public static final Identifier WELCOME_TO_OVERWORLD_ID = MinecraftMod.id("welcome_to_overworld");
	public static final Identifier PIGLIN_POOP_BARTER_ID = MinecraftMod.id("piglin_poop_barter");
	public static final Identifier NPC_ENDERMAN_ID = MinecraftMod.id("npc_enderman");
	public static final Identifier WITHER_SKULLS_ID = MinecraftMod.id("wither_skulls");
	public static final Identifier WITHER_SPAWN_ID = MinecraftMod.id("wither_spawn");
	public static final Identifier WITHER_KILL_ID = MinecraftMod.id("wither_kill");
	public static final Identifier BEACON_TAX_ID = MinecraftMod.id("beacon_tax");
	public static final Identifier READ_AND_ACCEPTED_ID = MinecraftMod.id("read_and_accepted");
	public static final String ROOT_CRITERION = "root";
	public static final String WELCOME_CRITERION = "welcome";
	public static final String GHAST_DEATH_CRITERION = "ghast_death";
	public static final String DIRT_SHOVEL_CRITERION = "dirt_shovel";
	public static final String WELCOME_TO_OVERWORLD_CRITERION = "welcome_to_overworld";
	public static final String PIGLIN_POOP_BARTER_CRITERION = "piglin_poop_barter";
	public static final String NPC_ENDERMAN_CRITERION = "npc_enderman";
	public static final String WITHER_SKULLS_CRITERION = "wither_skulls";
	public static final String WITHER_SPAWN_CRITERION = "wither_spawn";
	public static final String WITHER_KILL_CRITERION = "wither_kill";
	public static final String BEACON_TAX_CRITERION = "beacon_tax";
	public static final String READ_AND_ACCEPTED_CRITERION = "read_and_accepted";
	private static boolean loggedMissingWelcomeAdvancement;
	private static boolean loggedMissingDirtShovelAdvancement;
	private static final Set<UUID> GHAST_FIREBALL_BURNING_PLAYERS = ConcurrentHashMap.newKeySet();
	private static final ThreadLocal<ServerPlayer> BLOCK_PLACER = new ThreadLocal<>();

	public static net.minecraft.world.InteractionResult duringPlacement(ServerPlayer player, java.util.function.Supplier<net.minecraft.world.InteractionResult> action) {
		ServerPlayer previous = BLOCK_PLACER.get();
		BLOCK_PLACER.set(player);
		try {
			return action.get();
		} finally {
			if (previous == null) BLOCK_PLACER.remove();
			else BLOCK_PLACER.set(previous);
		}
	}

	public static void grantPlacedWither(Entity entity) {
		ServerPlayer player = BLOCK_PLACER.get();
		if (entity instanceof WitherBoss && player != null && player.level() == entity.level()) {
			grantWitherSpawned(player);
		}
	}

	public static void clearPlayer(UUID playerId) {
		GHAST_FIREBALL_BURNING_PLAYERS.remove(playerId);
	}

	public static void disableConflictingVanillaAdvancements(Map<Identifier, Advancement> advancements) {
		for (Identifier id : List.of(VANILLA_ENTER_NETHER_ID, VANILLA_SUMMON_WITHER_ID)) {
			advancements.computeIfPresent(id, (key, original) -> new Advancement(
				original.parent(), original.display(), original.rewards(),
				Map.of("disabled_by_minecraftmod", impossibleCriterion()),
				AdvancementRequirements.allOf(List.of("disabled_by_minecraftmod")), original.sendsTelemetryEvent()));
		}
	}

	private AdvancementRules() {
	}

	public static Advancement createRootAdvancement() {
		DisplayInfo display = new DisplayInfo(
			new ItemStack(Items.RED_NETHER_BRICKS),
			Component.literal("MinecraftMod"),
			Component.literal("Welcome to the HELL"),
			Optional.of(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace("gui/advancements/backgrounds/nether"))),
			AdvancementType.TASK,
			false,
			false,
			false
		);

		return new Advancement(
			Optional.empty(),
			Optional.of(display),
			AdvancementRewards.EMPTY,
			Map.of(ROOT_CRITERION, impossibleCriterion()),
			AdvancementRequirements.allOf(java.util.List.of(ROOT_CRITERION)),
			false
		);
	}

	public static Advancement createWelcomeToHellAdvancement() {
		DisplayInfo display = new DisplayInfo(
			new ItemStack(Items.RED_NETHER_BRICKS),
			Component.literal("Welcome to the HELL"),
			Component.literal("Oyunu ilk defa oyna ve ghastlara dikkat et"),
			Optional.empty(),
			AdvancementType.TASK,
			true,
			true,
			false
		);

		return new Advancement(
			Optional.of(ROOT_ID),
			Optional.of(display),
			AdvancementRewards.EMPTY,
			Map.of(WELCOME_CRITERION, PlayerTrigger.TriggerInstance.tick()),
			AdvancementRequirements.allOf(java.util.List.of(WELCOME_CRITERION)),
			false
		);
	}

	public static Advancement createGhastDeathAdvancement() {
		DisplayInfo display = new DisplayInfo(
			new ItemStack(Items.GHAST_TEAR),
			Component.literal("Ben sana dikkat et demiştim.."),
			Component.literal("Dünyada acımasızca ghast tarafından öl"),
			Optional.empty(),
			AdvancementType.TASK,
			true,
			true,
			false
		);

		return new Advancement(
			Optional.of(WELCOME_TO_HELL_ID),
			Optional.of(display),
			AdvancementRewards.EMPTY,
			Map.of(GHAST_DEATH_CRITERION, impossibleCriterion()),
			AdvancementRequirements.allOf(java.util.List.of(GHAST_DEATH_CRITERION)),
			false
		);
	}

	public static Advancement createDirtShovelAdvancement() {
		DisplayInfo display = new DisplayInfo(
			new ItemStack(Items.DIRT),
			Component.literal("Gerçek Hayatta Böyle"),
			Component.literal("Herhangi bir kürekle bir toprağı kır ve pişman ol"),
			Optional.empty(),
			AdvancementType.CHALLENGE,
			true,
			true,
			false
		);

		return new Advancement(
			Optional.of(WELCOME_TO_HELL_ID),
			Optional.of(display),
			AdvancementRewards.EMPTY,
			Map.of(DIRT_SHOVEL_CRITERION, impossibleCriterion()),
			AdvancementRequirements.allOf(java.util.List.of(DIRT_SHOVEL_CRITERION)),
			false
		);
	}

	public static Advancement createWelcomeToOverworldAdvancement() {
		return createHiddenAdvancement(
			DIRT_SHOVEL_ID,
			new ItemStack(Items.NETHERRACK),
			"Welcome to the OVERWORLD",
			"Nether ilk defa gir. D\u00fcnyaya ho\u015f geldin.",
			AdvancementType.TASK,
			0.0F,
			2.0F,
			WELCOME_TO_OVERWORLD_CRITERION
		);
	}

	public static Advancement createPiglinPoopBarterAdvancement() {
		return createHiddenAdvancement(
			WELCOME_TO_OVERWORLD_ID,
			new ItemStack(Items.ROOTED_DIRT),
			"Ender incisi nerede..?",
			"Ender incisi i\u00e7in ba\u015fka y\u00f6ntemler ara",
			AdvancementType.TASK,
			1.0F,
			3.0F,
			PIGLIN_POOP_BARTER_CRITERION
		);
	}

	public static Advancement createNpcEndermanAdvancement() {
		return createHiddenAdvancement(
			PIGLIN_POOP_BARTER_ID,
			new ItemStack(Items.ENDER_EYE),
			"M\u00fchendisten ne istedin?",
			"Katiller ender incisi alamaz. ba\u015fka y\u00f6ntemler bul",
			AdvancementType.TASK,
			2.0F,
			3.0F,
			NPC_ENDERMAN_CRITERION
		);
	}

	public static Advancement createWitherSkullsAdvancement() {
		return createHiddenAdvancement(
			NPC_ENDERMAN_ID,
			new ItemStack(Items.WITHER_SKELETON_SKULL),
			"\u015eimdi wither zaman\u0131",
			"3 Wither kafas\u0131 elde et ve ondan 12 ender incisi al.",
			AdvancementType.CHALLENGE,
			3.0F,
			3.0F,
			WITHER_SKULLS_CRITERION
		);
	}

	public static Advancement createWitherSpawnAdvancement() {
		return createHiddenAdvancement(
			WITHER_SKULLS_ID,
			new ItemStack(Items.SOUL_SAND),
			"Bu kadar kolay de\u011fil",
			"Bir wither do\u011fur ve CEHENNEM\u0130 YA\u015eA",
			AdvancementType.CHALLENGE,
			4.0F,
			3.0F,
			WITHER_SPAWN_CRITERION
		);
	}

	public static Advancement createWitherKillAdvancement() {
		return createHiddenAdvancement(
			WITHER_SPAWN_ID,
			new ItemStack(Items.NETHER_STAR),
			"Sen B\u0130R CANAVARSIN",
			"Bir wither \u00f6ld\u00fcr ve hayat\u0131n\u0131 sorgula",
			AdvancementType.CHALLENGE,
			5.0F,
			3.0F,
			WITHER_KILL_CRITERION
		);
	}

	public static Advancement createBeaconTaxAdvancement() {
		return createHiddenAdvancement(
			WITHER_KILL_ID,
			new ItemStack(Items.BEACON),
			"Fener mi? Vergiye ba\u011fland\u0131.",
			"Nether Y\u0131ld\u0131z\u0131'n\u0131 End Ge\u00e7idi vergisine teslim et",
			AdvancementType.CHALLENGE,
			6.0F,
			3.0F,
			BEACON_TAX_CRITERION
		);
	}

	public static Advancement createReadAndAcceptedAdvancement() {
		return createHiddenAdvancement(
			BEACON_TAX_ID,
			new ItemStack(Items.WRITTEN_BOOK),
			"Okudum ve Kabul Ediyorum",
			"End kullan\u0131m ko\u015fullar\u0131n\u0131 kabul et",
			AdvancementType.TASK,
			7.0F,
			3.0F,
			READ_AND_ACCEPTED_CRITERION
		);
	}

	public static void grantWelcomeToHell(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		AdvancementHolder root = level.getServer().getAdvancements().get(ROOT_ID);
		AdvancementHolder welcome = level.getServer().getAdvancements().get(WELCOME_TO_HELL_ID);
		if (root == null || welcome == null) {
			if (!loggedMissingWelcomeAdvancement) {
				MinecraftMod.LOGGER.warn("Welcome advancement could not be found: {} or {}", ROOT_ID, WELCOME_TO_HELL_ID);
				loggedMissingWelcomeAdvancement = true;
			}
			return;
		}

		if (!player.getAdvancements().getOrStartProgress(root).isDone()) {
			player.getAdvancements().award(root, ROOT_CRITERION);
		}

		if (player.getAdvancements().getOrStartProgress(welcome).isDone()) {
			return;
		}

		player.getAdvancements().award(welcome, WELCOME_CRITERION);
	}

	public static void grantGhastDeath(ServerPlayer player, DamageSource damageSource) {
		if (!isGhastDeath(player, damageSource) || !(player.level() instanceof ServerLevel level)) {
			return;
		}

		GHAST_FIREBALL_BURNING_PLAYERS.remove(player.getUUID());
		grantWelcomeToHell(player);

		AdvancementHolder advancement = level.getServer().getAdvancements().get(GHAST_DEATH_ID);
		if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
			return;
		}

		player.getAdvancements().award(advancement, GHAST_DEATH_CRITERION);
	}

	public static void grantDirtShovel(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		grantWelcomeToHell(player);

		AdvancementHolder advancement = level.getServer().getAdvancements().get(DIRT_SHOVEL_ID);
		if (advancement == null) {
			if (!loggedMissingDirtShovelAdvancement) {
				MinecraftMod.LOGGER.warn("Dirt shovel advancement could not be found: {}", DIRT_SHOVEL_ID);
				loggedMissingDirtShovelAdvancement = true;
			}
			return;
		}

		if (player.getAdvancements().getOrStartProgress(advancement).isDone()) {
			return;
		}

		player.getAdvancements().award(advancement, DIRT_SHOVEL_CRITERION);
	}

	public static void grantWelcomeToOverworldIfInNether(ServerPlayer player) {
		if (player.level().dimension() == Level.NETHER) {
			revokeVanillaEnterNether(player);
			grant(player, WELCOME_TO_OVERWORLD_ID, WELCOME_TO_OVERWORLD_CRITERION);
		}
	}

	public static void revokeVanillaEnterNether(ServerPlayer player) {
		revokeCompleted(player, VANILLA_ENTER_NETHER_ID);
	}

	public static void revokeVanillaSummonWither(ServerPlayer player) {
		revokeCompleted(player, VANILLA_SUMMON_WITHER_ID);
	}

	public static void grantPiglinPoopBarter(ServerPlayer player) {
		grant(player, PIGLIN_POOP_BARTER_ID, PIGLIN_POOP_BARTER_CRITERION);
	}

	public static void grantEndermanKilled(ServerPlayer player, Entity killedEntity) {
		if (killedEntity instanceof EnderMan) {
			grant(player, NPC_ENDERMAN_ID, NPC_ENDERMAN_CRITERION);
		}
	}

	public static void grantWitherSpawned(ServerPlayer player) {
		revokeVanillaSummonWither(player);
		grant(player, WITHER_SPAWN_ID, WITHER_SPAWN_CRITERION);
	}

	public static void grantWitherSpawnedNear(ServerLevel level, WitherBoss wither) {
		ServerPlayer nearestPlayer = null;
		double nearestDistance = 32.0D * 32.0D;
		for (ServerPlayer player : level.players()) {
			double distance = player.distanceToSqr(wither);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestPlayer = player;
			}
		}

		if (nearestPlayer != null) {
			grantWitherSpawned(nearestPlayer);
		}
	}

	public static void grantWitherKilled(ServerPlayer player, Entity killedEntity) {
		if (killedEntity instanceof WitherBoss) {
			grant(player, WITHER_KILL_ID, WITHER_KILL_CRITERION);
		}
	}

	public static void grantBeaconTax(ServerPlayer player) {
		grant(player, BEACON_TAX_ID, BEACON_TAX_CRITERION);
	}

	public static void grantReadAndAccepted(ServerPlayer player) {
		grant(player, READ_AND_ACCEPTED_ID, READ_AND_ACCEPTED_CRITERION);
	}

	public static void tickInventoryAdvancements(ServerPlayer player) {
		if (countItem(player, Items.WITHER_SKELETON_SKULL) >= 3) {
			grant(player, WITHER_SKULLS_ID, WITHER_SKULLS_CRITERION);
		}
	}

	public static void markGhastFireballBurn(ServerPlayer player) {
		GHAST_FIREBALL_BURNING_PLAYERS.add(player.getUUID());
	}

	public static void clearGhastFireballBurnIfNotBurning(ServerPlayer player) {
		if (player.getRemainingFireTicks() <= 0) {
			GHAST_FIREBALL_BURNING_PLAYERS.remove(player.getUUID());
		}
	}

	private static void revokeCompleted(ServerPlayer player, Identifier id) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		AdvancementHolder advancement = level.getServer().getAdvancements().get(id);
		if (advancement == null) {
			return;
		}

		List<String> completedCriteria = new ArrayList<>();
		for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria()) {
			completedCriteria.add(criterion);
		}

		for (String criterion : completedCriteria) {
			player.getAdvancements().revoke(advancement, criterion);
		}
	}

	private static boolean isGhastDeath(ServerPlayer player, DamageSource damageSource) {
		Entity source = damageSource.getEntity();
		Entity directSource = damageSource.getDirectEntity();
		return source instanceof Ghast
			|| directSource instanceof Ghast
			|| directSource instanceof Projectile projectile && projectile.getOwner() instanceof Ghast
			|| directSource instanceof TraceableEntity traceableEntity && traceableEntity.getOwner() instanceof Ghast
			|| GHAST_FIREBALL_BURNING_PLAYERS.contains(player.getUUID()) && FireRules.isFireDamage(damageSource);
	}

	private static Criterion<ImpossibleTrigger.TriggerInstance> impossibleCriterion() {
		return new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance());
	}

	private static Advancement createHiddenAdvancement(Identifier parent, ItemStack icon, String title, String description, AdvancementType type, float x, float y, String criterion) {
		DisplayInfo display = new DisplayInfo(
			icon,
			Component.literal(title),
			Component.literal(description),
			Optional.empty(),
			type,
			true,
			true,
			true
		);

		return new Advancement(
			Optional.of(parent),
			Optional.of(display),
			AdvancementRewards.EMPTY,
			Map.of(criterion, impossibleCriterion()),
			AdvancementRequirements.allOf(java.util.List.of(criterion)),
			false
		);
	}

	private static void grant(ServerPlayer player, Identifier id, String criterion) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		AdvancementHolder advancement = level.getServer().getAdvancements().get(id);
		if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) {
			return;
		}

		player.getAdvancements().award(advancement, criterion);
	}

	private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}
}
