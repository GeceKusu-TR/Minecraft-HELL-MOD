package com.example.minecraftmod;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class VillagerCombatRules {
	private static final double VISIBLE_PLAYER_RANGE = 20.0D;
	private static final double WITNESS_RANGE = 24.0D;
	private static final float TRIDENT_SPEED = 2.0F;
	private static final float TRIDENT_INACCURACY = 0.0F;

	private VillagerCombatRules() {
	}

	public static ItemStack createTrident(ServerLevel serverLevel) {
		ItemStack trident = new ItemStack(Items.TRIDENT);
		HolderGetter<Enchantment> enchantments = enchantmentGetter(serverLevel);
		trident.enchant(enchantments.getOrThrow(Enchantments.LOYALTY), 3);
		trident.enchant(enchantments.getOrThrow(Enchantments.IMPALING), 3);
		trident.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
		return trident;
	}

	private static HolderGetter<Enchantment> enchantmentGetter(ServerLevel serverLevel) {
		return (HolderGetter<Enchantment>) serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
	}

	public static void armVillager(Villager villager, ServerLevel serverLevel) {
		ItemStack mainHandItem = villager.getMainHandItem();
		if (mainHandItem.is(Items.TRIDENT)) {
			return;
		}

		villager.setItemSlot(EquipmentSlot.MAINHAND, createTrident(serverLevel));
	}

	public static Player findVisiblePlayer(ServerLevel serverLevel, Villager villager) {
		Player player = serverLevel.getNearestPlayer(villager, DifficultyScalingRules.more(VISIBLE_PLAYER_RANGE));
		if (player == null || !villager.hasLineOfSight(player)) {
			return null;
		}

		return player;
	}

	public static void throwTrident(ServerLevel serverLevel, Villager villager, Player target) {
		ItemStack projectileStack = villager.getMainHandItem().copy();
		ThrownTrident trident = new ThrownTrident(serverLevel, villager, projectileStack);
		Vec3 origin = villager.getEyePosition();
		Vec3 targetPosition = target.getEyePosition();
		trident.setPos(origin.x, origin.y, origin.z);
		trident.shoot(
			targetPosition.x - origin.x,
			targetPosition.y - origin.y,
			targetPosition.z - origin.z,
			(float)DifficultyScalingRules.more(TRIDENT_SPEED),
			TRIDENT_INACCURACY
		);
		serverLevel.addFreshEntity(trident);
	}

	public static void summonAngryGolem(ServerLevel serverLevel, Villager witness, Player target) {
		IronGolem golem = EntityType.IRON_GOLEM.create(serverLevel, EntitySpawnReason.EVENT);
		if (golem == null) {
			return;
		}

		golem.setPos(witness.getX(), witness.getY(), witness.getZ());
		golem.setPersistenceRequired();
		golem.setTarget(target);
		serverLevel.addFreshEntity(golem);
	}

	public static List<Villager> findWitnesses(ServerLevel serverLevel, Villager victim, Player killer) {
		double witnessRange = DifficultyScalingRules.more(WITNESS_RANGE);
		AABB searchBox = victim.getBoundingBox().inflate(witnessRange);
		return serverLevel.getEntitiesOfClass(Villager.class, searchBox, witness ->
			witness != victim
				&& witness.hasLineOfSight(killer)
				&& witness.distanceToSqr(killer) <= witnessRange * witnessRange
		);
	}
}
