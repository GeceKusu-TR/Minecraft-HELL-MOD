package com.example.minecraftmod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SkeletonLeaderRules {
	private static final String LEADER_TAG = "minecraftmod_skeleton_leader";
	private static final String GUARD_TAG_PREFIX = "minecraftmod_skeleton_guard:";
	private static final int TICK_INTERVAL = 20;
	private static final double FORMATION_RADIUS = 16.0D;
	private static final double PREFERRED_GUARD_RADIUS = 3.0D;
	private static final double MAX_GUARD_RADIUS = 6.0D;
	private static final int MAX_GUARDS = 10;
	private static final double LEADER_HEALTH_MULTIPLIER = 1.0D;
	private static final double GUARD_HEALTH_MULTIPLIER = 0.5D;
	private static final double LEADER_ACCURACY_MULTIPLIER = 1.5D;
	private static final double GUARD_SPREAD_MULTIPLIER = 0.7D;
	private static final net.minecraft.resources.Identifier LEADER_HEALTH_ID = MinecraftMod.id("skeleton_leader_health");
	private static final net.minecraft.resources.Identifier GUARD_HEALTH_ID = MinecraftMod.id("skeleton_guard_health");
	private static final DustParticleOptions LEADER_PARTICLES = new DustParticleOptions(0x8B0000, 0.7F);

	private SkeletonLeaderRules() {
	}

	public static void tick(ServerLevel level) {
		if (!isHardcoreOverworld(level)
			|| !PerformanceRules.shouldRunEveryTicks(level, "hardcore_skeleton_leaders", TICK_INTERVAL)) {
			return;
		}

		Set<AbstractSkeleton> skeletons = collectNearbySkeletons(level);
		if (skeletons.isEmpty()) {
			return;
		}

		List<AbstractSkeleton> leaders = new ArrayList<>();
		for (AbstractSkeleton skeleton : skeletons) {
			if (isLeader(skeleton)) {
				leaders.add(skeleton);
			}
		}

		for (AbstractSkeleton guard : skeletons) {
			if (!isGuard(guard)) {
				continue;
			}

			AbstractSkeleton leader = findGuardLeader(level, guard);
			if (leader == null || !leader.isAlive()) {
				clearRole(guard);
			}
		}

		for (AbstractSkeleton leader : leaders) {
			if (!leader.isAlive()) {
				continue;
			}

			List<AbstractSkeleton> guards = guardsOf(leader, skeletons);
			guards.sort(Comparator.comparingDouble(leader::distanceToSqr));
			while (guards.size() > MAX_GUARDS) {
				clearRole(guards.remove(guards.size() - 1));
			}

			for (AbstractSkeleton candidate : skeletons) {
				if (guards.size() >= MAX_GUARDS || candidate == leader || isLeader(candidate) || isGuard(candidate)) {
					continue;
				}
				if (candidate.distanceToSqr(leader) <= FORMATION_RADIUS * FORMATION_RADIUS) {
					markGuard(candidate, leader);
					guards.add(candidate);
				}
			}

			maintainLeader(level, leader, guards);
		}

		assignNewGroups(level, skeletons);
	}

	public static float adjustArrowInaccuracy(AbstractSkeleton skeleton, float original) {
		if (!(skeleton.level() instanceof ServerLevel level) || !isHardcoreOverworld(level)) {
			return original;
		}
		if (isLeader(skeleton)) {
			return (float)(original / LEADER_ACCURACY_MULTIPLIER);
		}
		if (isGuard(skeleton)) {
			return (float)(original * GUARD_SPREAD_MULTIPLIER);
		}
		return original;
	}

	private static void assignNewGroups(ServerLevel level, Set<AbstractSkeleton> skeletons) {
		Set<AbstractSkeleton> processed = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
		for (AbstractSkeleton skeleton : skeletons) {
			if (processed.contains(skeleton) || isLeader(skeleton) || isGuard(skeleton)) {
				continue;
			}

			List<AbstractSkeleton> group = new ArrayList<>();
			for (AbstractSkeleton candidate : skeletons) {
				if (!isLeader(candidate)
					&& !isGuard(candidate)
					&& candidate.distanceToSqr(skeleton) <= FORMATION_RADIUS * FORMATION_RADIUS) {
					group.add(candidate);
				}
			}

			if (group.size() < 2) {
				processed.add(skeleton);
				continue;
			}

			AbstractSkeleton leader = group.stream()
				.min(Comparator.comparingDouble(candidate -> distanceToNearestPlayer(level, candidate)))
				.orElse(skeleton);
			markLeader(leader);
			processed.addAll(group);

			group.stream()
				.filter(candidate -> candidate != leader)
				.sorted(Comparator.comparingDouble(leader::distanceToSqr))
				.limit(MAX_GUARDS)
				.forEach(candidate -> markGuard(candidate, leader));
		}
	}

	private static void maintainLeader(ServerLevel level, AbstractSkeleton leader, List<AbstractSkeleton> guards) {
		ServerPlayer target = SkeletonTargetingRules.findNearestVisiblePlayer(level, leader);
		if (target != null) {
			leader.setTarget(target);
			leader.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}

		if (level.getGameTime() % 10L == 0L) {
			level.sendParticles(LEADER_PARTICLES, leader.getX(), leader.getY() + 1.35D, leader.getZ(), 2, 0.25D, 0.35D, 0.25D, 0.0D);
		}

		for (AbstractSkeleton guard : guards) {
			if (!guard.isAlive()) {
				continue;
			}

			if (target != null && (guard.getTarget() == null || !guard.getTarget().isAlive())) {
				guard.setTarget(target);
			}

			double distanceSqr = guard.distanceToSqr(leader);
			if (distanceSqr > PREFERRED_GUARD_RADIUS * PREFERRED_GUARD_RADIUS) {
				guard.getNavigation().moveTo(leader, 1.25D);
			}
			if (distanceSqr > MAX_GUARD_RADIUS * MAX_GUARD_RADIUS) {
				pullInsideFormation(level, guard, leader);
			}
		}
	}

	private static void pullInsideFormation(ServerLevel level, AbstractSkeleton guard, AbstractSkeleton leader) {
		Vec3 fromLeader = guard.position().subtract(leader.position());
		Vec3 horizontal = new Vec3(fromLeader.x, 0.0D, fromLeader.z);
		if (horizontal.lengthSqr() < 1.0E-4D) {
			return;
		}

		Vec3 target = leader.position().add(horizontal.normalize().scale(5.5D));
		double oldX = guard.getX();
		double oldY = guard.getY();
		double oldZ = guard.getZ();
		guard.teleportTo(target.x, leader.getY(), target.z);
		if (!level.noCollision(guard)) {
			guard.teleportTo(oldX, oldY, oldZ);
			guard.getNavigation().moveTo(leader, 1.5D);
		}
	}

	private static List<AbstractSkeleton> guardsOf(AbstractSkeleton leader, Set<AbstractSkeleton> skeletons) {
		List<AbstractSkeleton> guards = new ArrayList<>();
		for (AbstractSkeleton skeleton : skeletons) {
			if (isGuardOf(skeleton, leader)) {
				guards.add(skeleton);
			}
		}
		return guards;
	}

	private static AbstractSkeleton findGuardLeader(ServerLevel level, AbstractSkeleton guard) {
		for (String tag : guard.getTags()) {
			if (!tag.startsWith(GUARD_TAG_PREFIX)) {
				continue;
			}
			try {
				if (level.getEntity(UUID.fromString(tag.substring(GUARD_TAG_PREFIX.length()))) instanceof AbstractSkeleton leader && isLeader(leader)) {
					return leader;
				}
			} catch (IllegalArgumentException ignored) {
				return null;
			}
		}
		return null;
	}

	private static boolean isGuardOf(AbstractSkeleton guard, AbstractSkeleton leader) {
		return guard.getTags().contains(GUARD_TAG_PREFIX + leader.getUUID());
	}

	private static void markLeader(AbstractSkeleton skeleton) {
		clearGuardTags(skeleton);
		skeleton.addTag(LEADER_TAG);
		applyHealthModifier(skeleton, LEADER_HEALTH_ID, LEADER_HEALTH_MULTIPLIER);
	}

	private static void markGuard(AbstractSkeleton skeleton, AbstractSkeleton leader) {
		if (isLeader(skeleton)) {
			return;
		}
		clearGuardTags(skeleton);
		skeleton.addTag(GUARD_TAG_PREFIX + leader.getUUID());
		applyHealthModifier(skeleton, GUARD_HEALTH_ID, GUARD_HEALTH_MULTIPLIER);
	}

	private static void clearRole(AbstractSkeleton skeleton) {
		skeleton.removeTag(LEADER_TAG);
		clearGuardTags(skeleton);
		AttributeInstance maxHealth = skeleton.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(LEADER_HEALTH_ID);
			maxHealth.removeModifier(GUARD_HEALTH_ID);
		}
		if (skeleton.getHealth() > skeleton.getMaxHealth()) {
			skeleton.setHealth(skeleton.getMaxHealth());
		}
	}

	private static void clearGuardTags(AbstractSkeleton skeleton) {
		for (String tag : new ArrayList<>(skeleton.getTags())) {
			if (tag.startsWith(GUARD_TAG_PREFIX)) {
				skeleton.removeTag(tag);
			}
		}
	}

	private static void applyHealthModifier(AbstractSkeleton skeleton, net.minecraft.resources.Identifier id, double amount) {
		AttributeInstance maxHealth = skeleton.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		if (maxHealth.getModifier(id) == null) {
			maxHealth.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			skeleton.setHealth(skeleton.getMaxHealth());
		}
	}

	private static Set<AbstractSkeleton> collectNearbySkeletons(ServerLevel level) {
		Set<AbstractSkeleton> skeletons = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || NoMobCommands.isNoMobEnabled(player)) {
				continue;
			}
			AABB area = player.getBoundingBox().inflate(48.0D);
			skeletons.addAll(level.getEntitiesOfClass(AbstractSkeleton.class, area, AbstractSkeleton::isAlive));
		}
		return skeletons;
	}

	private static double distanceToNearestPlayer(ServerLevel level, AbstractSkeleton skeleton) {
		double nearest = Double.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			if (!SkeletonTargetingRules.isValidTarget(player)) {
				continue;
			}
			nearest = Math.min(nearest, skeleton.distanceToSqr(player));
		}
		return nearest;
	}

	private static boolean isLeader(AbstractSkeleton skeleton) {
		return skeleton.getTags().contains(LEADER_TAG);
	}

	private static boolean isGuard(AbstractSkeleton skeleton) {
		for (String tag : skeleton.getTags()) {
			if (tag.startsWith(GUARD_TAG_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHardcoreOverworld(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && level.getLevelData().isHardcore();
	}
}
