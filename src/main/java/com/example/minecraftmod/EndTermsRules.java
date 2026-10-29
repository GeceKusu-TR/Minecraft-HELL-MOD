package com.example.minecraftmod;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class EndTermsRules {

    private static final String[] PAGES = {

        "1/18 END KULLANIM\nKOŞULLARI\n\nEnd'e girmeden önce\nşartları okumanız\ngerekir.\nOkumak, anlamak\ndeğildir.\nAnlamamak da ret\ndeğildir.\nNether Yıldızı verildi.\nGeri dönüş yok.",

        "2/18 TANIMLAR\n\nEnd:\nGeri dönmek\nisteyeceğiniz yer.\n\nOyuncu:\nFazla uğraşan kişi.\n\nEnderman:\nÇevre personeli.",

        "3/18 GİRİŞ HAKKI\n\nNether Yıldızı teslimi\ngiriş hakkı sağlamaz.\nSadece başvuruyu\naçar.\n\nİade edilmez.\nDeğiştirilmez.\nFener için geri\nalınamaz.\n\nGeçmiş olsun.",

        "4/18 PORTAL\nGARANTİSİ\n\nPortal zamanında\naçılmayabilir.\nAçık kalmayabilir.\nDoğru yere\ngötürmeyebilir.\nMoralinizi koruması\ngaranti edilmez.\n\nPortal yaklaşık\nçalışır.",

        "5/18 KAYIP EŞYALAR\n\nZırh, silah, XP,\nNetherit, yiyecek,\nElitra, Shulker Kutusu\nve yaşama sevinciniz\nkaybolabilir.\n\nYönetim sorumlu\ndeğildir.\n\nBoşluğa düşenler\nhâlâ düşmektedir.",

        "6/18 ENDERMAN\nPERSONELİ\n\nEndermanlar saldırgan\ndeğildir.\nOnlara bakabilirsiniz.\nGözlerinin içine de\nbakabilirsiniz.\nUzun uzun bile.\n\nGarip olması dışında\nbir yaptırımı yoktur.\nPersonel meşguldür.",

        "7/18 ÇEVRE\nDÜZENLEME\n\nEnderman personeli\nblokları izinsiz\nkaldırabilir.\n\nEvet.\nObsidiyen de dahil.",

		"8/18 SÖKÜLEN\nBLOKLAR\n\nAlınan bloklar kısa\nincelemeden sonra\nimha edilebilir.\nGeri yerleştirme\nyoktur.\n\n\"Tam altımdaki bloğu\naldı.\" şikâyeti hâlâ\naçıktır.",

        "9/18 ENDERMAN\nGÜVENLİĞİ\n\nEski bilgiler\ngeçersizdir.\nOk artık Enderman'a\nçarpar.\n\nEnderman öldürünce\nEnder incisi beklemeyin.\n\nŞiddet çözüm değildir.",

        "10/18 TNT\nPROSEDÜRÜ\n\nAktif TNT yanında\nEndermanların\ntoplanması normaldir.\n\nBu bir hata değildir.\nBu bir toplantıdır.",

        "11/18 EJDERHA\n\nEnder Ejderhası\nkıdemli müşteri\nilişkileri temsilcisidir.\n\nSizi hedefleyebilir.\nÜzerinize uçabilir.\nHücum edebilir.\nMüşteri memnuniyeti\nönceliğimizdir.",

        "12/18 END KRİSTALİ\n\nEnd Kristali kırmak\nyasak değildir.\nAncak tavsiye de\nedilmez.\n\nKristal kaybı kişisel\nalgılanmaktadır.",

        "13/18 ÖFKE\nSİSTEMİ\n\nHer kırılan kristal\nnefes saldırılarını\nsıklaştırabilir.\n\nÇok fazla kırılırsa:\nNefes. Nefes. Nefes. Nefes.\n\nSistem çalışıyor.",

        "14/18 EJDERHA\nNEFESİ\n\nEjderha nefesi sadece\nzarar vermek için\ndeğildir.\nAynı zamanda inşaat\nmalzemesidir.\nKaçışınızı, görüşünüzü\nve planınızı kapatır.\n\nÜcretsizdir.",

        "15/18 SAVAŞ\nGÜVENLİĞİ\n\nEjderha size saldırmak için yön değiştirebilir.\n\nKristal kırmak savaşı\nkolaylaştırırken başka\nkısımları zorlaştırır.\n\nBuna \"dinamik\nzorluk\" diyoruz.",

        "16/18 TEKNİK\nDESTEK\n\nSorun yaşarsanız\nteknik desteğe\nbaşvurabilirsiniz.\n\nHafta içi: Kapalıyız.\n\nHafta sonu: Yine kapalıyız.",

        "17/18 SON\nHÜKÜMLER\n\nEnderman yapınızı\nsökebilir.\nEnder incisi vermek\nzorunda değildir.\n\nNether Yıldızı iadesi\nyoktur.",

        "18/18 ONAY\n\nDoğrudan son sayfaya\ngeldiyseniz:\nSizi tanıyoruz.\n\nButona basarak tüm\nkoşulları ve henüz\nyazmadığımız koşulları\nkabul edersiniz."

	};

	private static final int PAGE_COUNT = PAGES.length;
	private static final String TERMS_BOOK_TAG = "minecraftmod_end_terms_book";
	private static final String TERMS_TOKEN_TAG = "minecraftmod_end_terms_token";
	private static final String TERMS_OPEN_PAGE_TAG = "minecraftmod_end_terms_open_page";
	private static final Map<UUID, TermsSession> SESSIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, PendingTermsStart> WAITING_FOR_SPACE = new ConcurrentHashMap<>();

	private EndTermsRules() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("kitap")
            .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
            .executes(context -> startFromCommand(context.getSource())));
		dispatcher.register(literal("minecraftmod_end_terms")
			.then(argument("page", integer(1, PAGE_COUNT))
				.executes(context -> handlePage(context.getSource(), getInteger(context, "page"), ""))
				.then(argument("token", word())
					.executes(context -> handlePage(context.getSource(), getInteger(context, "page"), getString(context, "token"))))));
	}

	private static int startFromCommand(CommandSourceStack source) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return 0;
		}

		start(player, () -> openEndPortalAtPlayer(player));
		return 1;
	}

	public static void start(ServerPlayer player, Runnable onAccepted) {
		TermsSession oldSession = SESSIONS.remove(player.getUUID());
		if (oldSession != null) {
			clearTermsBook(player, oldSession.token);
		}
		WAITING_FOR_SPACE.remove(player.getUUID());
		int selectedSlot = player.getInventory().getSelectedSlot();
		ItemStack selectedStack = player.getInventory().getSelectedItem();
		if (!selectedStack.isEmpty()) {
			int freeSlot = findSafeFreeSlot(player, selectedSlot);
			if (freeSlot < 0) {
				WAITING_FOR_SPACE.put(player.getUUID(), new PendingTermsStart(System.identityHashCode(player.level().getServer()), onAccepted));
				player.displayClientMessage(Component.literal("S\u00f6zle\u015fme kitab\u0131 i\u00e7in envanterde bo\u015f yer a\u00e7\u0131n."), false);
				return;
			}

			ItemStack preservedStack = selectedStack.copy();
			player.getInventory().setSelectedItem(ItemStack.EMPTY);
			player.getInventory().setItem(freeSlot, preservedStack);
		}

		TermsSession session = new TermsSession(selectedSlot, UUID.randomUUID().toString(), onAccepted);
		SESSIONS.put(player.getUUID(), session);
		openPage(player, 1);
	}

	public static void onDisconnect(ServerPlayer player) {
		TermsSession session = SESSIONS.remove(player.getUUID());
		if (session != null) {
			clearTermsBook(player, session.token);
		}
		WAITING_FOR_SPACE.remove(player.getUUID());
	}

	public static boolean isActive(ServerPlayer player) {
		return SESSIONS.containsKey(player.getUUID()) || isWaitingForSpace(player);
	}

	public static boolean isWaitingForSpace(ServerPlayer player) {
		PendingTermsStart pending = WAITING_FOR_SPACE.get(player.getUUID());
		return pending != null && pending.serverId == System.identityHashCode(player.level().getServer());
	}

	public static void retryIfWaitingForSpace(ServerPlayer player) {
		PendingTermsStart pending = WAITING_FOR_SPACE.get(player.getUUID());
		if (pending != null && pending.serverId == System.identityHashCode(player.level().getServer())) {
			start(player, pending.onAccepted);
		}
	}

	private static int handlePage(CommandSourceStack source, int page, String token) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return 0;
		}

		TermsSession session = SESSIONS.get(player.getUUID());
		if (session == null) {
			return 0;
		}

		if (!session.token.equals(token)) {
			player.displayClientMessage(Component.literal("L\u00fctfen s\u00f6zle\u015fmenin tamam\u0131n\u0131 okuyunuz."), false);
			openPage(player, session.read.nextClearBit(0) + 1);
			return 0;
		}

		int expectedPage = session.read.nextClearBit(0) + 1;
		if (page != expectedPage) {
			player.displayClientMessage(Component.literal("L\u00fctfen s\u00f6zle\u015fmenin tamam\u0131n\u0131 okuyunuz."), false);
			openPage(player, expectedPage);
			return 0;
		}

		if (page > 1 && !session.read.get(page - 2)) {
			player.displayClientMessage(Component.literal("L\u00fctfen s\u00f6zle\u015fmenin tamam\u0131n\u0131 okuyunuz."), false);
			openPage(player, session.read.nextClearBit(0) + 1);
			return 0;
		}

		session.read.set(page - 1);
		if (page < PAGE_COUNT) {
			openPage(player, page + 1);
			return 1;
		}

		if (session.read.nextClearBit(0) < PAGE_COUNT) {
			player.displayClientMessage(Component.literal("L\u00fctfen s\u00f6zle\u015fmenin tamam\u0131n\u0131 okuyunuz."), false);
			openPage(player, session.read.nextClearBit(0) + 1);
			return 0;
		}

		SESSIONS.remove(player.getUUID());
		WAITING_FOR_SPACE.remove(player.getUUID());
		clearTermsBook(player, session.token);
		AdvancementRules.grantReadAndAccepted(player);
		session.onAccepted.run();
		return 1;
	}

	private static void openPage(ServerPlayer player, int page) {
		TermsSession session = SESSIONS.get(player.getUUID());
		if (session == null) {
			return;
		}

		if (!prepareSelectedSlot(player, session)) {
			player.displayClientMessage(Component.literal("S\u00f6zle\u015fme kitab\u0131 i\u00e7in envanterde bo\u015f yer a\u00e7\u0131n."), false);
			return;
		}

		player.getInventory().setSelectedSlot(session.selectedSlot);
		player.getInventory().setSelectedItem(bookFor(session.token, page));
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
	}

	private static ItemStack bookFor(String token, int openPage) {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		List<Filterable<Component>> pages = new ArrayList<>();
		for (int i = 1; i <= PAGE_COUNT; i++) {
			pages.add(Filterable.passThrough(pageComponent(i, token)));
		}
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("End Kullan\u0131m Ko\u015fullar\u0131"),
			"End Y\u00f6netimi",
			0,
			pages,
			true
		));
		CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> {
			tag.putBoolean(TERMS_BOOK_TAG, true);
			tag.putString(TERMS_TOKEN_TAG, token);
			tag.putInt(TERMS_OPEN_PAGE_TAG, Math.max(1, Math.min(openPage, PAGE_COUNT)));
		});
		return book;
	}

	private static boolean prepareSelectedSlot(ServerPlayer player, TermsSession session) {
		ItemStack selectedStack = player.getInventory().getItem(session.selectedSlot);
		if (selectedStack.isEmpty() || isTermsBook(selectedStack, session.token)) {
			return true;
		}

		int freeSlot = findSafeFreeSlot(player, session.selectedSlot);
		if (freeSlot < 0) {
			return false;
		}

		player.getInventory().setItem(freeSlot, selectedStack.copy());
		player.getInventory().setItem(session.selectedSlot, ItemStack.EMPTY);
		return true;
	}

	private static int findSafeFreeSlot(ServerPlayer player, int selectedSlot) {
		for (int slot = 0; slot < 36; slot++) {
			if (slot != selectedSlot && player.getInventory().getItem(slot).isEmpty()) {
				return slot;
			}
		}
		return -1;
	}

	private static Component pageComponent(int page, String token) {
		Component button = Component.literal("\n[Kabul Ediyorum]")
			.withStyle(Style.EMPTY
				.withColor(ChatFormatting.DARK_GREEN)
				.withBold(true)
				.withClickEvent(new ClickEvent.RunCommand("/minecraftmod_end_terms " + page + " " + token)));
		return Component.literal(PAGES[page - 1]).append(button);
	}

	private static void clearTermsBook(ServerPlayer player, String token) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isTermsBook(stack, token)) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	private static boolean isTermsBook(ItemStack stack, String token) {
		if (stack.isEmpty()) {
			return false;
		}

		CompoundTag probe = new CompoundTag();
		probe.putBoolean(TERMS_BOOK_TAG, true);
		if (token != null && !token.isBlank()) {
			probe.putString(TERMS_TOKEN_TAG, token);
		}
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(probe);
	}

	private static void openEndPortalAtPlayer(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		BlockPos center = player.blockPosition();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				level.setBlock(center.offset(x, 0, z), Blocks.END_PORTAL.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		level.globalLevelEvent(1038, center, 0);
	}

	private static final class TermsSession {
		private final int selectedSlot;
		private final String token;
		private final Runnable onAccepted;
		private final BitSet read = new BitSet(PAGE_COUNT);

		private TermsSession(int selectedSlot, String token, Runnable onAccepted) {
			this.selectedSlot = selectedSlot;
			this.token = token;
			this.onAccepted = onAccepted;
		}
	}

	private record PendingTermsStart(int serverId, Runnable onAccepted) {
	}
}
