package com.example.minecraftmod;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FishingRules {
	public static final int UNLUCKY_FISHING_LEVEL = 255;
	public static final int FAST_FISHING_WAIT_TICKS = 20;
	private static final float FISHING_TRIDENT_DAMAGE = 10.0F;
	private static final double FISHING_TRIDENT_SPEED = 2.8D;
	private static final Map<UUID, UUID> FISHING_TRIDENT_TARGETS = new ConcurrentHashMap<>();

	private FishingRules() {
	}

	public static void enchantCraftedFishingRod(ServerLevel level, ItemStack stack) {
		if (!stack.is(Items.FISHING_ROD)) {
			return;
		}

		HolderGetter<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		stack.enchant(enchantments.getOrThrow(Enchantments.LUCK_OF_THE_SEA), UNLUCKY_FISHING_LEVEL);
	}

	public static void curseInventoryFishingRods(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			enchantIfNeeded(level, player.getInventory().getItem(slot));
		}
	}

	private static void enchantIfNeeded(ServerLevel level, ItemStack stack) {
		if (stack.is(Items.FISHING_ROD) && !isUnluckyFishingRod(level, stack)) {
			enchantCraftedFishingRod(level, stack);
		}
	}

	public static boolean isUnluckyFishingRod(ServerLevel level, ItemStack stack) {
		if (!stack.is(Items.FISHING_ROD)) {
			return false;
		}

		Holder<Enchantment> luckOfTheSea = level.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.LUCK_OF_THE_SEA);
		return stack.getEnchantments().getLevel(luckOfTheSea) >= UNLUCKY_FISHING_LEVEL;
	}

	public static boolean hasUnluckyFishingRod(ServerLevel level, net.minecraft.world.entity.player.Player player) {
		return isUnluckyFishingRod(level, player.getMainHandItem()) || isUnluckyFishingRod(level, player.getOffhandItem());
	}

	public static ObjectArrayList<ItemStack> worstFishingLoot(RandomSource random) {
		ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
		if (random.nextInt(10_000) == 0) {
			loot.add(new ItemStack(Items.BEDROCK));
			return loot;
		}

		ItemStack stack = switch (random.nextInt(9)) {
			case 0 -> new ItemStack(Items.ROTTEN_FLESH);
			case 1 -> new ItemStack(Items.STICK);
			case 2 -> new ItemStack(Items.STRING);
			case 3 -> new ItemStack(Items.BOWL);
			case 4 -> new ItemStack(Items.BONE);
			case 5 -> new ItemStack(Items.LEATHER);
			case 6 -> new ItemStack(Items.INK_SAC);
			case 7 -> new ItemStack(Items.TRIPWIRE_HOOK);
			default -> damagedLeatherBoots(random);
		};
		loot.add(stack);
		return loot;
	}

	public static void maybeThrowFishingTrident(ServerLevel level, ServerPlayer player, RandomSource random) {
		if (random.nextFloat() >= 0.75F) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		Vec3 side = new Vec3(-look.z, 0.0D, look.x);
		if (side.lengthSqr() < 1.0E-4D) {
			side = new Vec3(1.0D, 0.0D, 0.0D);
		}
		side = side.normalize().scale(random.nextBoolean() ? 2.5D : -2.5D);
		Vec3 origin = player.getEyePosition().add(look.scale(9.0D)).add(side).add(0.0D, 1.2D, 0.0D);

		ItemStack tridentStack = new ItemStack(Items.TRIDENT);
		ThrownTrident trident = new ThrownTrident(level, origin.x, origin.y, origin.z, tridentStack);
		trident.pickup = AbstractArrow.Pickup.DISALLOWED;
		trident.setBaseDamage(FISHING_TRIDENT_DAMAGE);
		trident.setNoGravity(true);
		homeFishingTrident(trident, player);
		FISHING_TRIDENT_TARGETS.put(trident.getUUID(), player.getUUID());
		level.addFreshEntity(trident);
	}

	public static boolean isFishingTrident(Entity entity) {
		return entity instanceof ThrownTrident && FISHING_TRIDENT_TARGETS.containsKey(entity.getUUID());
	}

	public static boolean tickFishingTrident(ThrownTrident trident) {
		UUID targetId = FISHING_TRIDENT_TARGETS.get(trident.getUUID());
		if (targetId == null) {
			return false;
		}
		if (!(trident.level() instanceof ServerLevel level)) {
			return false;
		}

		Entity target = level.getEntity(targetId);
		if (!(target instanceof ServerPlayer player) || player.isRemoved() || !player.isAlive()) {
			FISHING_TRIDENT_TARGETS.remove(trident.getUUID());
			trident.discard();
			return true;
		}

		homeFishingTrident(trident, player);
		return true;
	}

	public static boolean hurtWithFishingTrident(Entity trident, Entity hitEntity, DamageSource damageSource, float amount) {
		if (isFishingTrident(trident)) {
			FISHING_TRIDENT_TARGETS.remove(trident.getUUID());
			return hurtOnServer(hitEntity, damageSource, FISHING_TRIDENT_DAMAGE);
		}

		return hurtOnServer(hitEntity, damageSource, amount);
	}

	private static void homeFishingTrident(ThrownTrident trident, ServerPlayer target) {
		Vec3 toTarget = target.getEyePosition().subtract(trident.position());
		double distance = toTarget.length();
		if (distance <= 1.0E-4D) {
			return;
		}

		trident.setDeltaMovement(toTarget.scale(FISHING_TRIDENT_SPEED / distance));
	}

	private static ItemStack damagedLeatherBoots(RandomSource random) {
		ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
		boots.setDamageValue(boots.getMaxDamage() - 1 - random.nextInt(Math.max(1, boots.getMaxDamage() / 4)));
		return boots;
	}

	private static boolean hurtOnServer(Entity entity, DamageSource damageSource, float amount) {
		if (entity.level() instanceof ServerLevel level) {
			return entity.hurtServer(level, damageSource, amount);
		}
		return false;
	}
}
