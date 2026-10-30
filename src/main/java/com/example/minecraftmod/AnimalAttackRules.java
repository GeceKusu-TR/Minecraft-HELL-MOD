package com.example.minecraftmod;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class AnimalAttackRules {
	public static final double ANIMAL_HEALTH = 20.0D;
	private static final double TARGET_RANGE = 24.0D;
	private static final double HERD_ALERT_RADIUS = 8.0D;
	private static final int MAX_HERD_ATTACKERS = 3;
	private static final double ATTACK_RANGE_SQR = 4.0D;
	private static final double CHASE_SPEED = 1.25D;
	private static final double DIRECT_CHASE_PUSH = 0.08D;
	private static final int ATTACK_INTERVAL_TICKS = 10;
	public static final float UNARMORED_ATTACK_DAMAGE = 2.0F;
	public static final float FULL_LEATHER_FINAL_ATTACK_DAMAGE = 1.0F;
	public static final float FULL_ARMOR_FINAL_ATTACK_DAMAGE = 2.0F;
	public static final float FULL_LEATHER_REFLECT_DAMAGE = 20.0F;

	private AnimalAttackRules() {
	}

	public static boolean canTarget(Player player) {
		return player.isAlive() && player.level().getDifficulty() != Difficulty.PEACEFUL && !player.isCreative() && !player.isSpectator();
	}

	public static void ensureAnimalHealth(Animal animal) {
		AttributeInstance maxHealth = animal.getAttribute(Attributes.MAX_HEALTH);
		double scaledHealth = DifficultyScalingRules.more(ANIMAL_HEALTH);
		if (maxHealth == null || maxHealth.getBaseValue() == scaledHealth) {
			return;
		}

		float oldMaxHealth = animal.getMaxHealth();
		maxHealth.setBaseValue(scaledHealth);
		if (animal.getHealth() >= oldMaxHealth || animal.getHealth() > animal.getMaxHealth()) {
			animal.setHealth((float)scaledHealth);
		}
	}

	public static void tickAngryAnimal(Animal animal) {
		if (!(animal.level() instanceof ServerLevel level)) {
			return;
		}
		ensureAnimalHealth(animal);
		if (level.getDifficulty() == Difficulty.PEACEFUL || !animal.isAlive() || animal.isBaby()) {
			if (animal.getTarget() instanceof Player) {
				animal.setTarget(null);
				animal.getNavigation().stop();
			}
			return;
		}

		Player target = nearestTarget(level, animal);
		if (target == null) {
			animal.setTarget(null);
			return;
		}
		if (target instanceof ServerPlayer serverPlayer && NoMobCommands.isNoMobEnabled(serverPlayer)) {
			animal.discard();
			return;
		}

		animal.setTarget(target);
		animal.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (animal.tickCount % DifficultyScalingRules.sooner(10) == 0 || animal.getNavigation().isDone()) {
			animal.getNavigation().moveTo(target, DifficultyScalingRules.more(CHASE_SPEED));
		}
		pushTowardTarget(animal, target);
		if (animal.distanceToSqr(target) <= DifficultyScalingRules.more(ATTACK_RANGE_SQR)
			&& animal.hasLineOfSight(target)
			&& animal.tickCount % DifficultyScalingRules.sooner(ATTACK_INTERVAL_TICKS) == 0) {
			performAnimalAttack(level, animal, target);
		}
	}

	public static boolean performAnimalAttack(ServerLevel level, Animal animal, Player player) {
		if (!canTarget(player)) {
			return false;
		}

		animal.swing(InteractionHand.MAIN_HAND);
		if (LeatherHelmetRules.hasFullLeatherArmorSet(player)) {
			// Yes, leather set again hahaahahah.
			boolean hurt = player.hurtServer(level, animal.damageSources().mobAttack(animal), DifficultyScalingRules.more(UNARMORED_ATTACK_DAMAGE));
			if (hurt) {
				hurtAnimalForLeatherSet(level, animal, player);
			}
			return hurt;
		}

		return player.hurtServer(level, animal.damageSources().mobAttack(animal), DifficultyScalingRules.more(UNARMORED_ATTACK_DAMAGE));
	}

	public static float attackDamageFor(Player player, float originalAmount) {
		if (LeatherHelmetRules.hasFullLeatherArmorSet(player)) {
			// Yes, leather set again hahaahahah.
			return rawDamageForTargetFinalDamage(player, DifficultyScalingRules.more(FULL_LEATHER_FINAL_ATTACK_DAMAGE));
		}

		if (hasFullArmorSet(player)) {
			return rawDamageForTargetFinalDamage(player, DifficultyScalingRules.more(FULL_ARMOR_FINAL_ATTACK_DAMAGE));
		}

		return Math.max(originalAmount, DifficultyScalingRules.more(UNARMORED_ATTACK_DAMAGE));
	}

	public static void hurtAnimalForLeatherSet(ServerLevel level, LivingEntity animal, Player player) {
		if (LeatherHelmetRules.hasFullLeatherArmorSet(player)) {
			animal.hurtServer(level, player.damageSources().thorns(player), DifficultyScalingRules.more(FULL_LEATHER_REFLECT_DAMAGE));
		}
	}

	public static void triggerHerdAttack(ServerLevel level, Animal victim, Player attacker) {
		if (!victim.isAlive() || victim.isBaby() || !canTarget(attacker)) {
			return;
		}

		int alertedAnimals = 0;
		for (Animal animal : level.getEntitiesOfClass(
			Animal.class,
			victim.getBoundingBox().inflate(DifficultyScalingRules.more(HERD_ALERT_RADIUS)),
			candidate -> candidate != victim
				&& candidate.isAlive()
				&& !candidate.isBaby()
				&& candidate.getTarget() != attacker
		)) {
			animal.setTarget(attacker);
			alertedAnimals++;
			if (alertedAnimals >= MAX_HERD_ATTACKERS) {
				return;
			}
		}
	}

	private static boolean hasFullArmorSet(Player player) {
		return !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
			&& !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
			&& !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
			&& !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
	}

	private static Player nearestTarget(ServerLevel level, Animal animal) {
		Player nearest = null;
		double targetRange = DifficultyScalingRules.more(TARGET_RANGE);
		double nearestDistance = targetRange * targetRange;
		for (ServerPlayer player : level.players()) {
			if (!canTarget(player)) {
				continue;
			}

			double distance = animal.distanceToSqr(player);
			if (distance < nearestDistance) {
				nearest = player;
				nearestDistance = distance;
			}
		}

		return nearest;
	}

	private static void pushTowardTarget(Animal animal, Player target) {
		net.minecraft.world.phys.Vec3 delta = target.position().subtract(animal.position()).multiply(1.0D, 0.0D, 1.0D);
		if (delta.lengthSqr() < 1.0E-4D) {
			return;
		}

		net.minecraft.world.phys.Vec3 movement = animal.getDeltaMovement();
		net.minecraft.world.phys.Vec3 chase = delta.normalize().scale(DIRECT_CHASE_PUSH);
		animal.setDeltaMovement(movement.x * 0.65D + chase.x, movement.y, movement.z * 0.65D + chase.z);
		animal.hurtMarked = true;
	}

	private static float rawDamageForTargetFinalDamage(Player player, float targetFinalDamage) {
		float low = targetFinalDamage;
		float high = 200.0F;
		for (int i = 0; i < 24; i++) {
			float mid = (low + high) * 0.5F;
			if (estimatedFinalDamage(player, mid) < targetFinalDamage) {
				low = mid;
			} else {
				high = mid;
			}
		}

		return high;
	}

	private static float estimatedFinalDamage(Player player, float rawDamage) {
		float armor = player.getArmorValue();
		float toughness = (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		float armorReduction = Math.min(20.0F, Math.max(armor / 5.0F, armor - rawDamage / (2.0F + toughness / 4.0F))) / 25.0F;
		float afterArmor = rawDamage * (1.0F - armorReduction);
		float protectionReduction = Math.min(20, protectionLevel(player)) / 25.0F;
		return afterArmor * (1.0F - protectionReduction);
	}

	private static int protectionLevel(Player player) {
		HolderLookup.RegistryLookup<Enchantment> enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> protection = enchantments.getOrThrow(Enchantments.PROTECTION);
		return EnchantmentHelper.getItemEnchantmentLevel(protection, player.getItemBySlot(EquipmentSlot.HEAD))
			+ EnchantmentHelper.getItemEnchantmentLevel(protection, player.getItemBySlot(EquipmentSlot.CHEST))
			+ EnchantmentHelper.getItemEnchantmentLevel(protection, player.getItemBySlot(EquipmentSlot.LEGS))
			+ EnchantmentHelper.getItemEnchantmentLevel(protection, player.getItemBySlot(EquipmentSlot.FEET));
	}
}
