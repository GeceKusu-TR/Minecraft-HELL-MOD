package com.example.minecraftmod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class EndPortalRitualRules {
	private static final int RITUAL_TICKS = 400;
	private static final int WINDOWS_PAUSE_TICKS = 60;
	private static final int STILL_SIGNAL_TICKS = 60;
	private static final int MAX_DISTANCE = 20;
	private static final String PAID_TAG_PREFIX = "minecraftmod_end_portal_paid_";
	private static final int[] FRAME_X = {-1, 0, 1, -1, 0, 1, -2, -2, -2, 2, 2, 2};
	private static final int[] FRAME_Z = {-2, -2, -2, 2, 2, 2, -1, 0, 1, -1, 0, 1};
	private static final Map<ServerLevel, Map<Long, Session>> SESSIONS = new WeakHashMap<>();
	private static final Map<ServerLevel, Set<Long>> PAID_SITES = new WeakHashMap<>();

	private EndPortalRitualRules() {
	}

	public static boolean shouldHijackTwelfthEye(ServerLevel level, BlockPos clickedPos) {
		return findCompleteSite(level, clickedPos, true) != null;
	}

	public static void placeTwelfthEyeAndStart(ServerLevel level, BlockPos clickedPos, ServerPlayer player) {
		BlockState state = level.getBlockState(clickedPos);
		if (state.is(Blocks.END_PORTAL_FRAME) && !state.getValue(EndPortalFrameBlock.HAS_EYE)) {
			level.setBlock(clickedPos, state.setValue(EndPortalFrameBlock.HAS_EYE, true), Block.UPDATE_ALL);
			level.updateNeighbourForOutputSignal(clickedPos, Blocks.END_PORTAL_FRAME);
			level.levelEvent(1503, clickedPos, 0);
		}

		startFromFrame(level, clickedPos, player);
	}

	public static boolean startFromFrame(ServerLevel level, BlockPos framePos, ServerPlayer player) {
		PortalSite site = findCompleteSite(level, framePos, false);
		if (site == null) {
			return false;
		}

		long key = site.center().asLong();
		Map<Long, Session> levelSessions = SESSIONS.computeIfAbsent(level, ignored -> new HashMap<>());
		Session existing = levelSessions.get(key);
		if (existing != null || hasOpenPortal(level, site.center())) {
			return false;
		}

		boolean starPaid = PAID_SITES.getOrDefault(level, Set.of()).contains(key) || player.getTags().contains(paidTag(key));
		Session session = new Session(site.center(), player.getUUID(), starPaid);
		levelSessions.put(key, session);
		clearPortalBlocks(level, site.center());
		return true;
	}

	public static void tick(ServerLevel level) {
		Map<Long, Session> sessions = SESSIONS.get(level);
		if (sessions == null || sessions.isEmpty()) {
			return;
		}

		sessions.entrySet().removeIf(entry -> tickSession(level, entry.getKey(), entry.getValue()));
	}

	private static boolean tickSession(ServerLevel level, long key, Session session) {
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(session.owner());
		if (owner == null || owner.level() != level || owner.distanceToSqr(session.center().getCenter()) > MAX_DISTANCE * MAX_DISTANCE) {
			if (owner != null) {
				owner.sendSystemMessage(Component.literal("Ba\u011flant\u0131 kesildi."));
				EndTermsRules.onDisconnect(owner);
			}
			clearPortalBlocks(level, session.center());
			return true;
		}

		if (session.completed()) {
			if (session.starPaid() && !session.termsStarted()) {
				session.setTermsStarted(true);
				EndTermsRules.start(owner, () -> openPortal(level, owner, key, session));
				return false;
			}

			if (session.starPaid()) {
				if (EndTermsRules.isWaitingForSpace(owner) && level.getGameTime() % 20L == 0L) {
					EndTermsRules.retryIfWaitingForSpace(owner);
				}
				if (!EndTermsRules.isActive(owner)) {
					session.setTermsStarted(false);
				}
				return false;
			}

			owner.displayClientMessage(Component.literal("Portal\u0131n ortas\u0131na Nether Y\u0131ld\u0131z\u0131 at\u0131n\u0131z.."), true);
			ItemEntity star = findNetherStar(level, session.center());
			if (star != null) {
				star.getItem().shrink(1);
				if (star.getItem().isEmpty()) {
					star.discard();
				}
				PAID_SITES.computeIfAbsent(level, ignored -> new HashSet<>()).add(key);
				owner.addTag(paidTag(key));
				session.setStarPaid(true);
				AdvancementRules.grantBeaconTax(owner);
				session.setTermsStarted(true);
				EndTermsRules.start(owner, () -> openPortal(level, owner, key, session));
			}
			return false;
		}

		session.setTicks(session.ticks() + 1);
		if (session.ticks() % 80 == 0) {
			spawnWave(level, session.center());
		}

		if (session.ticks() <= RITUAL_TICKS) {
			int progress = Math.max(1, Math.min(99, session.ticks() * 99 / RITUAL_TICKS));
			owner.displayClientMessage(Component.literal("End Ge\u00e7idi ba\u015flat\u0131l\u0131yor... %" + progress), true);
			return false;
		}

		if (!session.windowsMessageSent()) {
			owner.sendSystemMessage(Component.literal("Windows g\u00fcncellemeleri yap\u0131land\u0131r\u0131l\u0131yor..."));
			session.setWindowsMessageSent(true);
		}

		owner.displayClientMessage(Component.literal("End Ge\u00e7idi ba\u015flat\u0131l\u0131yor... %99"), true);
		if (session.ticks() >= RITUAL_TICKS + WINDOWS_PAUSE_TICKS) {
			if (!session.connectingMessageSent()) {
				owner.sendSystemMessage(Component.literal("Sunucuya ba\u011flan\u0131l\u0131yor..."));
				session.setConnectingMessageSent(true);
			}

			int signal = signalStrength(owner, session.center());
			if (signal < 3) {
				session.setStillTicks(0);
				owner.displayClientMessage(Component.literal("Sinyal g\u00fcc\u00fc: " + signalBars(signal)), true);
				return false;
			}

			if (owner.blockPosition().equals(session.lastSignalPos())) {
				session.setStillTicks(session.stillTicks() + 1);
			} else {
				session.setLastSignalPos(owner.blockPosition());
				session.setStillTicks(0);
			}

			if (session.stillTicks() < STILL_SIGNAL_TICKS) {
				owner.displayClientMessage(Component.literal("Sinyal g\u00fcc\u00fc: " + signalBars(signal)), true);
				return false;
			}

			session.setCompleted(true);
		}
		return false;
	}

	private static void openPortal(ServerLevel level, ServerPlayer owner, long key, Session session) {
		Map<Long, Session> sessions = SESSIONS.get(level);
		if (sessions == null || sessions.get(key) != session) {
			return;
		}

		BlockPos min = session.center().offset(-1, 0, -1);
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				level.setBlock(min.offset(x, 0, z), Blocks.END_PORTAL.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.globalLevelEvent(1038, session.center(), 0);
		dropReceipt(level, session.center());
		sessions.remove(key);
		owner.sendSystemMessage(Component.literal("Portal a\u00e7\u0131ld\u0131."));
	}

	private static void dropReceipt(ServerLevel level, BlockPos center) {
		ItemStack receipt = new ItemStack(Items.PAPER);
		receipt.set(DataComponents.CUSTOM_NAME, Component.literal("END GE\u00c7\u0130\u015e BELGES\u0130").withStyle(ChatFormatting.WHITE));
		receipt.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal("Portal: Kale #" + (1000 + level.random.nextInt(9000))),
			Component.literal("Nether Y\u0131ld\u0131z\u0131: 1"),
			Component.literal("\u0130ade: Yok")
		)));
		Block.popResource(level, center.above(), receipt);
	}

	private static void clearPortalBlocks(ServerLevel level, BlockPos center) {
		BlockPos min = center.offset(-1, 0, -1);
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				BlockPos pos = min.offset(x, 0, z);
				if (level.getBlockState(pos).is(Blocks.END_PORTAL)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
	}

	private static boolean hasOpenPortal(ServerLevel level, BlockPos center) {
		BlockPos min = center.offset(-1, 0, -1);
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				if (level.getBlockState(min.offset(x, 0, z)).is(Blocks.END_PORTAL)) {
					return true;
				}
			}
		}
		return false;
	}

	private static ItemEntity findNetherStar(ServerLevel level, BlockPos center) {
		AABB area = new AABB(center).inflate(1.75D);
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, item -> item.isAlive() && item.getItem().is(Items.NETHER_STAR))) {
			return item;
		}
		return null;
	}

	private static String paidTag(long key) {
		return PAID_TAG_PREFIX + Long.toUnsignedString(key);
	}

	private static void spawnWave(ServerLevel level, BlockPos center) {
		for (int i = 0; i < 4; i++) {
			spawn(level, center, i % 2 == 0 ? EntityType.SILVERFISH : EntityType.ZOMBIE);
		}
	}

	private static void spawn(ServerLevel level, BlockPos center, EntityType<? extends Monster> type) {
		Monster monster = type.create(level, EntitySpawnReason.EVENT);
		if (monster == null) {
			return;
		}

		int x = center.getX() + level.random.nextInt(17) - 8;
		int z = center.getZ() + level.random.nextInt(17) - 8;
		BlockPos spawnPos = new BlockPos(x, center.getY() + 1, z);
		monster.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
		monster.setYRot(level.random.nextFloat() * 360.0F);
		level.addFreshEntity(monster);
	}

	private static int signalStrength(ServerPlayer player, BlockPos center) {
		BlockPos bestSpot = center.offset(2, 0, 2);
		double distance = player.distanceToSqr(bestSpot.getCenter());
		if (distance <= 2.25D) return 5;
		if (distance <= 9.0D) return 4;
		if (distance <= 25.0D) return 3;
		if (distance <= 64.0D) return 2;
		return 1;
	}

	private static String signalBars(int strength) {
		return "\u25ae".repeat(Math.max(0, strength)) + "\u25af".repeat(Math.max(0, 5 - strength));
	}

	private static PortalSite findCompleteSite(ServerLevel level, BlockPos framePos, boolean includeClickedEye) {
		for (int cx = framePos.getX() - 2; cx <= framePos.getX() + 2; cx++) {
			for (int cz = framePos.getZ() - 2; cz <= framePos.getZ() + 2; cz++) {
				BlockPos center = new BlockPos(cx, framePos.getY(), cz);
				if (isCompleteRing(level, center, framePos, includeClickedEye)) {
					return new PortalSite(center);
				}
			}
		}
		return null;
	}

	private static boolean isCompleteRing(ServerLevel level, BlockPos center, BlockPos clickedPos, boolean includeClickedEye) {
		for (int i = 0; i < FRAME_X.length; i++) {
			BlockPos frame = center.offset(FRAME_X[i], 0, FRAME_Z[i]);
			BlockState state = level.getBlockState(frame);
			if (!state.is(Blocks.END_PORTAL_FRAME)) {
				return false;
			}
			if (!state.getValue(EndPortalFrameBlock.HAS_EYE) && !(includeClickedEye && frame.equals(clickedPos))) {
				return false;
			}
		}
		return true;
	}

	private record PortalSite(BlockPos center) {
	}

	private static final class Session {
		private final BlockPos center;
		private final UUID owner;
		private int ticks;
		private int stillTicks;
		private BlockPos lastSignalPos = BlockPos.ZERO;
		private boolean completed;
		private boolean windowsMessageSent;
		private boolean connectingMessageSent;
		private boolean starPaid;
		private boolean termsStarted;

		private Session(BlockPos center, UUID owner, boolean starPaid) {
			this.center = center;
			this.owner = owner;
			this.starPaid = starPaid;
		}

		private BlockPos center() { return center; }
		private UUID owner() { return owner; }
		private int ticks() { return ticks; }
		private void setTicks(int ticks) { this.ticks = ticks; }
		private int stillTicks() { return stillTicks; }
		private void setStillTicks(int stillTicks) { this.stillTicks = stillTicks; }
		private BlockPos lastSignalPos() { return lastSignalPos; }
		private void setLastSignalPos(BlockPos lastSignalPos) { this.lastSignalPos = lastSignalPos; }
		private boolean completed() { return completed; }
		private void setCompleted(boolean completed) { this.completed = completed; }
		private boolean windowsMessageSent() { return windowsMessageSent; }
		private void setWindowsMessageSent(boolean windowsMessageSent) { this.windowsMessageSent = windowsMessageSent; }
		private boolean connectingMessageSent() { return connectingMessageSent; }
		private void setConnectingMessageSent(boolean connectingMessageSent) { this.connectingMessageSent = connectingMessageSent; }
		private boolean starPaid() { return starPaid; }
		private void setStarPaid(boolean starPaid) { this.starPaid = starPaid; }
		private boolean termsStarted() { return termsStarted; }
		private void setTermsStarted(boolean termsStarted) { this.termsStarted = termsStarted; }
	}
}
