package com.example.minecraftmod;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DragonRules {
	private static final int BASE_FLAME_CYCLE_TICKS = 67;
	private static final int MIN_FLAME_CYCLE_TICKS = 5;
	private static final int FLAME_CYCLE_REDUCTION_PER_CRYSTAL = 4;
	private static final int OBSIDIAN_WALL_HALF_WIDTH = 4;
	private static final int OBSIDIAN_WALL_HEIGHT = 5;
	private static final double FORCED_BREATH_RANGE_SQR = 50.0D * 50.0D;
	private static final double FORCED_BREATH_FORWARD_OFFSET = 4.0D;
	private static final int SIDE_WALL_GAP_HALF_WIDTH = 1;
	private static final Map<ServerLevel, Integer> BROKEN_CRYSTALS = new WeakHashMap<>();
	private static final Map<ServerLevel, Long> LAST_FORCED_BREATH_TICKS = new WeakHashMap<>();

	private DragonRules() {
	}

	public static void tick(ServerLevel serverLevel) {
		if (!isEndDragonAggressionEnabled(serverLevel)) {
			return;
		}

		EnderDragon dragon = findActiveDragon(serverLevel);
		if (dragon == null) {
			return;
		}

		long gameTime = serverLevel.getGameTime();
		Long lastBreathTick = LAST_FORCED_BREATH_TICKS.get(serverLevel);
		if (lastBreathTick != null && gameTime - lastBreathTick < flameCycleTicks(dragon)) {
			return;
		}

		ServerPlayer target = findNearestDragonTarget(serverLevel, dragon.position(), FORCED_BREATH_RANGE_SQR);
		if (target == null) {
			return;
		}

		LAST_FORCED_BREATH_TICKS.put(serverLevel, gameTime);
		spawnForcedBreathCloud(serverLevel, dragon, target);
	}

	public static boolean isEndDragonAggressionEnabled(ServerLevel serverLevel) {
		return serverLevel.dimension() == Level.END
			&& serverLevel.getDifficulty() != Difficulty.PEACEFUL;
	}

	public static boolean isEligibleDragonTarget(Player player) {
		return player.mayBuild()
			&& !player.isCreative()
			&& !player.isSpectator();
	}

	public static ServerPlayer findNearestDragonTarget(ServerLevel serverLevel, Vec3 origin) {
		return findNearestDragonTarget(serverLevel, origin, Double.MAX_VALUE);
	}

	public static ServerPlayer findNearestDragonTarget(ServerLevel serverLevel, Vec3 origin, double maxDistanceSqr) {
		ServerPlayer nearestPlayer = null;
		double nearestDistance = Double.MAX_VALUE;

		for (ServerPlayer player : serverLevel.players()) {
			if (!isEligibleDragonTarget(player)) {
				continue;
			}

			double distance = player.distanceToSqr(origin.x, origin.y, origin.z);
			if (distance <= maxDistanceSqr && distance < nearestDistance) {
				nearestDistance = distance;
				nearestPlayer = player;
			}
		}

		return nearestPlayer;
	}

	public static int flameCycleTicks(EnderDragon dragon) {
		if (!(dragon.level() instanceof ServerLevel serverLevel) || !isEndDragonAggressionEnabled(serverLevel)) {
			return BASE_FLAME_CYCLE_TICKS;
		}

		int brokenCrystals = BROKEN_CRYSTALS.getOrDefault(serverLevel, 0);
		return Math.max(MIN_FLAME_CYCLE_TICKS, BASE_FLAME_CYCLE_TICKS - brokenCrystals * FLAME_CYCLE_REDUCTION_PER_CRYSTAL);
	}

	public static void onDragonBreathCloudCreated(ServerLevel level, EnderDragon dragon, AreaEffectCloud cloud) {
		if (!isEndDragonAggressionEnabled(level) || cloud == null) {
			return;
		}

		buildObsidianBreathWall(level, dragon.position(), cloud.position());
	}

	public static void onCrystalDestroyed(ServerLevel level) {
		if (!isEndDragonAggressionEnabled(level)) {
			return;
		}

		int brokenCrystals = BROKEN_CRYSTALS.getOrDefault(level, 0) + 1;
		BROKEN_CRYSTALS.put(level, brokenCrystals);
	}

	private static void buildObsidianBreathWall(ServerLevel level, Vec3 dragonPos, Vec3 cloudPos) {
		BlockPos center = BlockPos.containing(cloudPos);
		boolean wallRunsAlongZ = Math.abs(cloudPos.x - dragonPos.x) >= Math.abs(cloudPos.z - dragonPos.z);

		for (int width = -OBSIDIAN_WALL_HALF_WIDTH; width <= OBSIDIAN_WALL_HALF_WIDTH; width++) {
			if (Math.abs(width) <= SIDE_WALL_GAP_HALF_WIDTH) {
				continue;
			}

			for (int height = 0; height < OBSIDIAN_WALL_HEIGHT; height++) {
				BlockPos pos = wallRunsAlongZ
					? center.offset(0, height, width)
					: center.offset(width, height, 0);
				placeObsidianIfAllowed(level, pos);
			}
		}
	}

	private static EnderDragon findActiveDragon(ServerLevel level) {
		for (var entity : level.getAllEntities()) {
			if (entity instanceof EnderDragon dragon && dragon.isAlive()) {
				return dragon;
			}
		}

		return null;
	}

	private static void spawnForcedBreathCloud(ServerLevel level, EnderDragon dragon, ServerPlayer target) {
		Vec3 targetPos = target.position();
		Vec3 attackDirection = targetPos.subtract(dragon.position()).multiply(1.0D, 0.0D, 1.0D);
		if (attackDirection.lengthSqr() < 1.0E-4D) {
			attackDirection = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		}
		if (attackDirection.lengthSqr() < 1.0E-4D) {
			attackDirection = new Vec3(1.0D, 0.0D, 0.0D);
		}

		Vec3 cloudPos = targetPos.add(attackDirection.normalize().scale(FORCED_BREATH_FORWARD_OFFSET));
		BlockPos.MutableBlockPos floorSearchPos = new BlockPos.MutableBlockPos(cloudPos.x, cloudPos.y, cloudPos.z);
		double cloudY = cloudPos.y;
		while (cloudY > level.getMinY() && level.isEmptyBlock(floorSearchPos)) {
			cloudY--;
			floorSearchPos.set(cloudPos.x, cloudY, cloudPos.z);
		}

		cloudY = Math.floor(cloudY) + 1.0D;
		AreaEffectCloud cloud = new AreaEffectCloud(level, cloudPos.x, cloudY, cloudPos.z);
		cloud.setOwner(dragon);
		cloud.setRadius(5.0F);
		cloud.setDuration(200);
		cloud.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
		cloud.setPotionDurationScale(0.25F);
		cloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE));
		level.addFreshEntity(cloud);
		buildObsidianBreathWall(level, dragon.position(), cloud.position());
	}

	private static void placeObsidianIfAllowed(ServerLevel level, BlockPos pos) {
		var state = level.getBlockState(pos);
		if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) {
			return;
		}

		level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
	}
}
