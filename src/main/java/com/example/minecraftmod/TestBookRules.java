package com.example.minecraftmod;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.GameType;

public final class TestBookRules {
	private static final String[] BOOK_TITLES = {
		"Kullanım Koşulları",
		"Gizlilik Sözleşmesi",
		"Risk ve Sorumluluk Reddi"
	};
	private static final String[] BOOK_AUTHORS = {
		"Hukuk Departmanı",
		"Gizlilik Departmanı",
		"Sorumluluk Kabul Etmeme Departmanı"
	};
	private static final String[][] BOOK_PAGES = {bookOnePages(), bookTwoPages(), bookThreePages()};
	private static final int[] PAGE_COUNTS = {BOOK_PAGES[0].length, BOOK_PAGES[1].length, BOOK_PAGES[2].length};
	private static final String TEST_BOOK_TAG = "minecraftmod_test_book";
	private static final String TEST_BOOK_TOKEN_TAG = "minecraftmod_test_book_token";
	private static final String TEST_BOOK_OPEN_PAGE_TAG = "minecraftmod_test_book_open_page";
	private static final String COMPLETED_PLAYER_TAG = "minecraftmod_test_books_completed";
	private static final String PROGRESS_TAG_PREFIX = "minecraftmod_test_books_progress_";
	private static final Map<UUID, TestBookSession> SESSIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> WAITING_FOR_SPACE = new ConcurrentHashMap<>();

	private TestBookRules() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("minecraftmod_test_book")
			.then(argument("book", integer(1, PAGE_COUNTS.length))
				.then(argument("page", integer(1, PAGE_COUNTS[PAGE_COUNTS.length - 1]))
					.then(argument("token", word())
						.executes(context -> acceptPage(
							context.getSource(),
							getInteger(context, "book"),
							getInteger(context, "page"),
							getString(context, "token")
						))))));
	}

	public static boolean tick(ServerPlayer player) {
		if (isCreativeCreatedWorld(player)) {
			SESSIONS.remove(player.getUUID());
			WAITING_FOR_SPACE.remove(player.getUUID());
			clearProgressTags(player);
			clearTestBook(player, null);
			player.addTag(COMPLETED_PLAYER_TAG);
			return false;
		}

		if (player.getTags().contains(COMPLETED_PLAYER_TAG)) {
			WAITING_FOR_SPACE.remove(player.getUUID());
			clearTestBook(player, null);
			return false;
		}

		TestBookSession session = SESSIONS.get(player.getUUID());
		if (session != null && session.serverId != System.identityHashCode(player.level().getServer())) {
			SESSIONS.remove(player.getUUID());
			session = null;
		}

		if (session == null) {
			if (player.tickCount < 5) {
				return false;
			}
			session = startOrResume(player);
			if (session == null) {
				clearTestBook(player, null);
				WAITING_FOR_SPACE.put(player.getUUID(), System.identityHashCode(player.level().getServer()));
				player.displayClientMessage(Component.literal("Zorunlu kitap i\u00e7in envanterde bo\u015f yer a\u00e7\u0131n."), true);
				player.setDeltaMovement(0.0D, 0.0D, 0.0D);
				player.hurtMarked = true;
				return true;
			}
		}

		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.hurtMarked = true;
		if (session.activePlayer.get() != player) {
			session.activePlayer = new WeakReference<>(player);
			openCurrentBook(player, session);
		}
		return true;
	}

	public static boolean isReadingRequiredBooks(ServerPlayer player) {
		TestBookSession session = SESSIONS.get(player.getUUID());
		int serverId = System.identityHashCode(player.level().getServer());
		return session != null && session.serverId == serverId || WAITING_FOR_SPACE.getOrDefault(player.getUUID(), 0) == serverId;
	}

	public static void onDisconnect(ServerPlayer player) {
		SESSIONS.remove(player.getUUID());
		WAITING_FOR_SPACE.remove(player.getUUID());
	}

	private static TestBookSession startOrResume(ServerPlayer player) {
		Progress progress = progressFromTags(player);
		int selectedSlot = player.getInventory().getSelectedSlot();
		int freeSlot = findSafeFreeSlot(player, selectedSlot);
		int movedSlot = -1;
		ItemStack selectedStack = player.getInventory().getSelectedItem();
		if (!selectedStack.isEmpty() && !isTestBook(selectedStack, null)) {
			if (freeSlot < 0) {
				return null;
			}

			ItemStack preservedStack = selectedStack.copy();
			player.getInventory().setSelectedItem(ItemStack.EMPTY);
			player.getInventory().setItem(freeSlot, preservedStack);
			movedSlot = freeSlot;
		}

		clearTestBook(player, null);
		TestBookSession session = new TestBookSession(
			System.identityHashCode(player.level().getServer()),
			selectedSlot,
			movedSlot,
			UUID.randomUUID().toString(),
			progress.book(),
			progress.nextPage()
		);
		SESSIONS.put(player.getUUID(), session);
		WAITING_FOR_SPACE.remove(player.getUUID());
		saveProgress(player, session.book, session.nextPage);
		openCurrentBook(player, session);
		session.activePlayer = new WeakReference<>(player);
		return session;
	}

	private static int acceptPage(CommandSourceStack source, int book, int page, String token) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return 0;
		}

		TestBookSession session = SESSIONS.get(player.getUUID());
		if (session == null || session.serverId != System.identityHashCode(player.level().getServer()) || !session.token.equals(token) || book != session.book) {
			return 0;
		}

		int pageCount = PAGE_COUNTS[session.book - 1];
		if (page < 1 || page > pageCount) {
			return 0;
		}

		if (page != session.nextPage) {
			openCurrentBook(player, session);
			return 0;
		}

		if (page < pageCount) {
			session.nextPage = page + 1;
			saveProgress(player, session.book, session.nextPage);
			return 1;
		}

		clearTestBook(player, session.token);
		if (session.book < PAGE_COUNTS.length) {
			session.book++;
			session.nextPage = 1;
			saveProgress(player, session.book, session.nextPage);
			openCurrentBook(player, session);
			session.activePlayer = new WeakReference<>(player);
			return 1;
		}

		SESSIONS.remove(player.getUUID());
		WAITING_FOR_SPACE.remove(player.getUUID());
		clearProgressTags(player);
		restoreMovedItem(player, session);
		player.addTag(COMPLETED_PLAYER_TAG);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		return 1;
	}

	private static boolean isCreativeCreatedWorld(ServerPlayer player) {
		return player.level().getServer().getDefaultGameType() == GameType.CREATIVE;
	}

	private static void openCurrentBook(ServerPlayer player, TestBookSession session) {
		if (!prepareSelectedBookSlot(player, session)) {
			player.displayClientMessage(Component.literal("Zorunlu kitap i\u00e7in envanterde bo\u015f yer a\u00e7\u0131n."), true);
			return;
		}

		player.getInventory().setSelectedSlot(session.selectedSlot);
		player.getInventory().setSelectedItem(createBook(session.book, session.token, session.nextPage));
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
	}

	private static ItemStack createBook(int bookNumber, String token, int openPage) {
		int pageCount = PAGE_COUNTS[bookNumber - 1];
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		List<Filterable<Component>> pages = new ArrayList<>();
		for (int page = 1; page <= pageCount; page++) {
			pages.add(Filterable.passThrough(pageComponent(bookNumber, page, pageCount, token)));
		}

		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(BOOK_TITLES[bookNumber - 1]),
			BOOK_AUTHORS[bookNumber - 1],
			0,
			pages,
			true
		));
		CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> {
			tag.putBoolean(TEST_BOOK_TAG, true);
			tag.putString(TEST_BOOK_TOKEN_TAG, token);
			tag.putInt(TEST_BOOK_OPEN_PAGE_TAG, Math.max(1, Math.min(openPage, pageCount)));
		});
		return book;
	}

	private static Component pageComponent(int bookNumber, int page, int pageCount, String token) {
		Component button = Component.literal("\n[KABUL EDİYORUM]")
			.withStyle(Style.EMPTY
				.withColor(ChatFormatting.DARK_GREEN)
				.withBold(true)
				.withClickEvent(new ClickEvent.RunCommand("/minecraftmod_test_book " + bookNumber + " " + page + " " + token)));
		return Component.literal(BOOK_PAGES[bookNumber - 1][page - 1]).append(button);
	}

	private static String[] bookOnePages() {
		return new String[] {
			"""
			1/5

			KULLANIM KOŞULLARI

			Bu dünyada
			yaptığınız,
			yapmadığınız ve
			yanlışlıkla yaptığınız
			her şey size aittir.

			Geliştiriciye
			bakmayınız.
			""",
			"""
			2/5

			SAVUNMALAR

			"Bilmiyordum."

			"Bu kesin bug."

			geçerli savunmalar
			değildir.
			""",
			"""
			3/5

			ÇALIŞMA DURUMU

			Çalışıyorsa:
			Normaldir.
			Çalışmıyorsa:
			Normaldir.
			İkisini birden
			yapıyorsa:

			Daha normaldir.
			""",
			"""
			4/5

			ŞİKAYETLER

			"LAN"

			"NASIL?"

			"BEN DOKUNMADIM"

			resmi şikayet
			sayılmaz.
			""",
			"""
			5/5

			SON HÜKÜM

			Adalet, mantık ve
			merhamet garantisi
			verilmemektedir.

			Buraya kadar
			geldiyseniz artık
			çok geç.
			"""
		};
	}

	private static String[] bookTwoPages() {
		return new String[] {
			"""
			1/5

			GİZLİLİK

			Bu belge gizlidir.
			Size verilmiştir.
			Artık pek gizli
			değildir.

			Bunun sorumluluğu
			size aittir.
			""",
			"""
			2/5

			KİŞİSEL BİLGİLER

			Adınızı, adresinizi
			ve kart bilgilerinizi
			istemiyoruz.

			Lütfen vermeyin.
			Gerçekten.
			""",
			"""
			3/5

			KAYITLAR

			Gerekli bilgiler
			hatırlanabilir.
			Hangileri gerekli?

			Reklamlardan sonra..
			""",
			"""
			4/5

			GÖZETİM

			Düşüncelerinizi
			okuyamıyoruz.
			Şimdilik :)

			(Bu bir şakadır.
			Hukuk Departmanı
			böyle yazdırdı)
			""",
			"""
			5/5

			SON HÜKÜM

			Ekrana bakarken
			yaptığınız yüzler
			kaydedilmez.

			Tahmin edilebilmesi
			bizim suçumuz
			değildir.
			"""
		};
	}

	private static String[] bookThreePages() {
		return new String[] {
			"""
			1/199

			RİSK VE
			SORUMLULUK REDDİ

			Bu belgeyi okumak
			zorunludur.

			Maalesef.
			""",
			"""
			2/199

			Beklenmedik olaylar
			meydana gelebilir.

			Beklenen olaylar da
			beklenmedik olabilir.
			""",
			"""
			3/199

			Modun adil olması
			garanti edilmez.

			Mantıklı olması da
			garanti edilmez.
			""",
			"""
			4/199

			Güvenlik garantisi
			verilmemektedir.

			Huzur da.
			""",
			"""
			5/199

			Yaptığınız
			şeylerden siz
			sorumlusunuz.

			Şaşırtıcı şekilde.
			""",
			"""
			6/199

			Yapmadığınız
			şeylerden de bazen
			siz sorumlu
			olabilirsiniz.

			Detaya girmeyelim.
			""",
			"""
			7/199

			"Bilmiyordum."

			Savunmanız
			incelendi.

			REDDEDİLDİ.
			""",
			"""
			8/199

			"Nasıl
			bilebilirdim?"

			Güzel soru.

			Sonraki madde.
			""",
			"""
			9/199

			Cevap:

			Bilemezdiniz.

			Bu durum sonucu
			değiştirmez.
			""",
			"""
			10/199

			"Bu kesin bug."

			resmi başvuru
			değildir.
			""",
			"""
			11/199

			Bug olabilir.

			Olmayabilir.

			Burada karar
			vermiyoruz.
			""",
			"""
			12/199

			Bir şey normal
			görünüyorsa:

			Şimdilik
			normaldir.
			""",
			"""
			13/199

			Bir şey anormal
			görünüyorsa:

			Gözlem
			başarılıdır.
			""",
			"""
			14/199

			Hiçbir şey
			olmuyorsa:

			Şüphelidir.
			""",
			"""
			15/199

			Çok fazla şey
			oluyorsa:

			Daha şüphelidir.
			""",
			"""
			16/199

			Geliştiriciye
			bakmayınız.

			Bu yardımcı
			olmaz.
			""",
			"""
			17/199

			Ekrana
			bakabilirsiniz.

			Ekran muhtemelen
			masumdur.
			""",
			"""
			18/199

			Klavyeyi suçlamak
			serbesttir.

			Sonuç değişmez.
			""",
			"""
			19/199

			Fare masum kabul
			edilir.

			Şimdilik.
			""",
			"""
			20/199

			Sandalyeyi
			olaylara
			karıştırmayınız.

			Hiç suçu yok.
			""",
			"""
			21/199

			ŞİKAYET
			PROSEDÜRÜ

			Önce sakin olun.
			""",
			"""
			22/199

			Sakin olamadınız mı?

			Anlaşılabilir.

			Devam edin.
			""",
			"""
			23/199

			Şikayetinizi
			yüksek sesle
			söyleyebilirsiniz.
			""",
			"""
			24/199

			Kimse duymayabilir.

			Bu teknik bir
			sorun değildir.
			""",
			"""
			25/199

			"LAN"

			geçerli başvuru
			formatı değildir.
			""",
			"""
			26/199

			"NASIL?"

			da değildir.
			""",
			"""
			27/199

			"OLAMAZ."

			itiraz olarak
			kaydedilmez.
			""",
			"""
			28/199

			"BEN HİÇBİR ŞEY
			YAPMADIM."

			Şüpheli ifade.
			""",
			"""
			29/199

			"AMA NORMAL
			MINECRAFT'TA..."

			Durun.
			""",
			"""
			30/199

			Cümlenin devamını
			duymamıza gerek
			yoktur.
			""",
			"""
			31/199

			"Wiki'de böyle
			yazıyordu."

			Wiki burada
			çalışmıyor.
			""",
			"""
			32/199

			Arkadaşınız:

			"Bir şey olmaz."

			dediyse bu
			garanti değildir.
			""",
			"""
			33/199

			Arkadaşınız yanlış
			çıktıysa onunla
			konuşunuz.
			""",
			"""
			34/199

			Bizi araya
			sokmayınız.

			Teşekkürler.
			""",
			"""
			35/199

			"Deneyeyim."

			ifadesi risk
			kabulüdür.
			""",
			"""
			36/199

			"Ne olacak ki?"

			daha güçlü bir
			risk kabulüdür.
			""",
			"""
			37/199

			"En kötü ne
			olabilir?"

			Artık bizlik
			değilsiniz.
			""",
			"""
			38/199

			Sonrasında olanlar
			tesadüf olabilir.
			""",
			"""
			39/199

			Tesadüf olmayabilir
			de.

			Açıklama yoktur.
			""",
			"""
			40/199

			40 madde bitti.

			159 kaldı.

			Geçmiş olsun.
			""",
			"""
			41/199

			GARANTİ
			BÖLÜMÜ

			Hazır olun.
			""",
			"""
			42/199

			Garanti kapsamındaki
			durumlar:








			""",
			"""
			43/199






			Liste devam ediyor.
			""",
			"""
			44/199






			Hala devam ediyor.
			""",
			"""
			45/199






			Liste sona erdi.
			""",
			"""
			46/199

			Anlamadıysanız:

			Garanti yok.
			""",
			"""
			47/199

			Hiç mi?

			Hiç.
			""",
			"""
			48/199

			Emin miyiz?

			Oldukça.
			""",
			"""
			49/199

			Küçücük bir
			garanti?

			Hayır.
			""",
			"""
			50/199

			Bir adet:

			"Geçmiş olsun."

			sunabiliriz.
			""",
			"""
			51/199

			Kullanıcı başına
			azami:

			1 geçmiş olsun.
			""",
			"""
			52/199

			İkinci
			"geçmiş olsun"

			verilmez.

			Stok politikası.
			""",
			"""
			53/199

			Garanti başlangıç
			tarihi:

			Bilinmiyor.
			""",
			"""
			54/199

			Garanti bitiş
			tarihi:

			Bilinmiyor.
			""",
			"""
			55/199

			Muhtemelen çoktan
			bitti.

			Üzücü.
			""",
			"""
			56/199

			Garanti belgesini
			aramayın.

			Bulamazsınız.
			""",
			"""
			57/199

			Çünkü garanti
			belgesi yok.
			""",
			"""
			58/199

			İyi haber:

			58. sayfadasınız.
			""",
			"""
			59/199

			Kötü haber:

			140 sayfa
			daha var.
			""",
			"""
			60/199

			Okumaya devam
			etmek zorunludur.

			Başarılar.
			""",
			"""
			61/199

			SORUMLULUK
			BÖLÜMÜ

			Kararlar size
			aittir.
			""",
			"""
			62/199

			Kötü kararlar da
			size aittir.
			""",
			"""
			63/199

			Çok kötü kararlar
			özellikle.
			""",
			"""
			64/199

			Karar verirken
			gülmeniz sonucu
			değiştirmez.
			""",
			"""
			65/199

			Önce düşünmeniz
			önerilir.
			""",
			"""
			66/199

			Düşünüp yine
			yaptıysanız:

			Geçmiş olsun.
			""",
			"""
			67/199

			Yanlışlıkla yapılan
			şey de yapılmış
			sayılır.
			""",
			"""
			68/199

			"Elim çarptı."

			Resmi cevap:

			Üzücü.
			""",
			"""
			69/199

			"Yanlış tuşa
			bastım."

			Tuş doğrulaması
			yoktur.
			""",
			"""
			70/199

			"Refleksti."

			Refleksinizle
			konuşunuz.
			""",
			"""
			71/199

			Panikte verilen
			kararlar da
			karardır.
			""",
			"""
			72/199

			Panik yapmamanız
			önerilir.
			""",
			"""
			73/199

			Bu önerinin işe
			yaradığı garanti
			edilmez.
			""",
			"""
			74/199

			Araştırmayı
			planlamıyoruz.
			""",
			"""
			75/199

			Bir şeyi önceden
			bilememeniz
			normaldir.
			""",
			"""
			76/199

			Bilmeniz de sizi
			kurtarmayabilir.
			""",
			"""
			77/199

			Bilip yine
			yaptıysanız:

			Tebrikler.
			""",
			"""
			78/199

			Tebrik ödülü:

			Yok.
			""",
			"""
			79/199

			Ceza var mı?

			Burada yazmıyor.
			""",
			"""
			80/199

			Bu cümleyi fazla
			incelemeyiniz.
			""",
			"""
			81/199

			DESTEK BÖLÜMÜ

			Prosedür
			başlıyor.
			""",
			"""
			82/199

			Adım 1:

			Sorunu fark edin.
			""",
			"""
			83/199

			Adım 2:

			Soruna bakın.
			""",
			"""
			84/199

			Adım 3:

			Sessizce bekleyin.
			""",
			"""
			85/199

			Adım 4:

			"Ne?"

			deyin.
			""",
			"""
			86/199

			Adım 5:

			Tekrar bakın.
			""",
			"""
			87/199

			Hala oradaysa:

			Şaşırın.
			""",
			"""
			88/199

			Bize ulaşmayı
			düşünün.
			""",
			"""
			89/199

			Düşünmeniz
			yeterlidir.
			""",
			"""
			90/199

			Destek prosedürü
			tamamlandı.
			""",
			"""
			91/199

			Sorun çözüldü mü?

			Muhtemelen hayır.
			""",
			"""
			92/199

			Prosedür
			uygulandı mı?

			Evet.
			""",
			"""
			93/199

			O halde sistem
			başarılıdır.
			""",
			"""
			94/199

			İtiraz etmek
			serbesttir.
			""",
			"""
			95/199

			İtiraz formu:

			Mevcut değildir.
			""",
			"""
			96/199

			Forma itiraz için
			aynı form gerekir.
			""",
			"""
			97/199

			Form olmadığı için
			başvuru oluşmadı.
			""",
			"""
			98/199

			Başvuru yoksa
			sorun da kayıtlarda
			yoktur.
			""",
			"""
			99/199

			Sorun çözülmüş
			sayılmıştır.

			Tebrikler.
			""",
			"""
			100/199

			100. MADDE

			Buraya kadar
			geldiniz.

			Neden?
			""",
			"""
			101/199

			ACİL DURUM
			PROSEDÜRÜ

			Başlıyor.
			""",
			"""
			102/199

			1.

			Panik yapmayın.
			""",
			"""
			103/199

			2.

			Ne olduğunu
			anlamaya çalışın.
			""",
			"""
			104/199

			3.

			Anlayamayın.
			""",
			"""
			105/199

			4.

			Tekrar deneyin.
			""",
			"""
			106/199

			5.

			Daha da az
			anlayın.
			""",
			"""
			107/199

			6.

			Panik yapın.
			""",
			"""
			108/199

			7.

			Panik yapmamanız
			gerektiğini
			hatırlayın.
			""",
			"""
			109/199

			8.

			Geç kaldınız.
			""",
			"""
			110/199

			Acil durum
			prosedürü
			tamamlandı.
			""",
			"""
			111/199

			Başarılı mı?

			Tanımlanmadı.
			""",
			"""
			112/199

			Başarısız mı?

			O da tanımlanmadı.
			""",
			"""
			113/199

			Sonuç:

			Başarılımsı.
			""",
			"""
			114/199

			"Başarılımsı"

			resmi bir kelime
			değildir.
			""",
			"""
			115/199

			Artık öyle.
			""",
			"""
			116/199

			Sözleşme yeni
			kelime üretme
			hakkına sahiptir.
			""",
			"""
			117/199

			Anlamlarını
			sorabilirsiniz.
			""",
			"""
			118/199

			Cevap vermek
			zorunda değiliz.
			""",
			"""
			119/199

			Rahatsız oldunuz mu?

			Normal.
			""",
			"""
			120/199

			Rahatladıysanız:

			Biraz garip.
			""",
			"""
			121/199

			MANTIK BÖLÜMÜ

			Mantık kullanmak
			serbesttir.
			""",
			"""
			122/199

			İşe yaraması
			garanti edilmez.
			""",
			"""
			123/199

			Sebep ve sonuç
			bulunabilir.
			""",
			"""
			124/199

			Bulunmayabilir.
			""",
			"""
			125/199

			Bulduğunuzu
			düşünebilirsiniz.
			""",
			"""
			126/199

			Yanılıyor
			olabilirsiniz.
			""",
			"""
			127/199

			Haklı da
			olabilirsiniz.
			""",
			"""
			128/199

			Bu konuda karar
			vermiyoruz.
			""",
			"""
			129/199

			Geçmiş deneyimler
			geleceği garanti
			etmez.
			""",
			"""
			130/199

			Gelecek de geçmişi
			düzeltmez.
			""",
			"""
			131/199

			Şu anki deneyim?

			Şu an oluyor.
			""",
			"""
			132/199

			Bu açıklama
			yeterlidir.

			Bizce.
			""",
			"""
			133/199

			Bir şey bir kez
			olduysa tekrar
			olabilir.
			""",
			"""
			134/199

			Olmayabilir de.
			""",
			"""
			135/199

			Hiç olmadıysa:

			Henüz olmamıştır.
			""",
			"""
			136/199

			Bu değerli bilgi
			ücretsizdir.
			""",
			"""
			137/199

			Bu maddeyi tekrar
			okuyabilirsiniz.
			""",
			"""
			138/199

			İstemiyorsanız:

			Zaten okuyorsunuz.
			""",
			"""
			139/199

			138. maddeyi
			kanıtladınız.

			Teşekkürler.
			""",
			"""
			140/199

			59 sayfa kaldı.

			Çok az.

			Göreceli olarak.
			""",
			"""
			141/199

			KAYIP BÖLÜMÜ

			Kayıplar
			mümkündür.
			""",
			"""
			142/199

			Eşya kaybı
			olabilir.
			""",
			"""
			143/199

			Zaman kaybı
			olabilir.
			""",
			"""
			144/199

			Moral kaybı
			olabilir.
			""",
			"""
			145/199

			Sabır kaybı
			olabilir.
			""",
			"""
			146/199

			Ne yaptığınızı
			unutabilirsiniz.
			""",
			"""
			147/199

			Neden yaptığınızı
			da unutabilirsiniz.
			""",
			"""
			148/199

			Oyunu neden
			açtığınızı bile
			unutabilirsiniz.
			""",
			"""
			149/199

			Bundan geliştirici
			sorumlu değildir.
			""",
			"""
			150/199

			Biz de bazen
			unutuyoruz.
			""",
			"""
			151/199

			Kayıp prosedürü:

			"Vah vah."
			""",
			"""
			152/199

			Değerli kayıpsa:

			"Çok vah vah."
			""",
			"""
			153/199

			Çok önemliyse:

			...
			""",
			"""
			154/199

			Sessizlik de
			destek sayılabilir.
			""",
			"""
			155/199

			Geri ödeme:

			0
			""",
			"""
			156/199

			İade:

			0
			""",
			"""
			157/199

			Tazminat:

			0
			""",
			"""
			158/199

			Teselli:

			Duruma göre.
			""",
			"""
			159/199

			Sarılma:

			Teknik olarak
			sağlanamıyor.
			""",
			"""
			160/199

			Geçmiş olsun.

			Kotanız kullanıldı.
			""",
			"""
			161/199

			DUYGUSAL HASAR

			Sinirlenebilirsiniz.
			""",
			"""
			162/199

			Bu doğal bir
			davranıştır.
			""",
			"""
			163/199

			Masaya vurmayınız.
			""",
			"""
			164/199

			Vurduysanız:

			Masa suçsuzdur.
			""",
			"""
			165/199

			Klavyeye de
			vurmayınız.
			""",
			"""
			166/199

			Vurduysanız kendi
			garantisine bakın.
			""",
			"""
			167/199

			Bizim garantimize
			değil.
			""",
			"""
			168/199

			Bizde garanti yok.

			Bunu konuşmuştuk.
			""",
			"""
			169/199

			Ekrana bağırmak
			işe yaramaz.
			""",
			"""
			170/199

			Daha yüksek sesle
			bağırmak da
			işe yaramaz.
			""",
			"""
			171/199

			Denemiş
			olabilirsiniz.

			Yargılamıyoruz.
			""",
			"""
			172/199

			Biraz
			yargılıyoruz.
			""",
			"""
			173/199

			Ama resmi olarak
			değil.
			""",
			"""
			174/199

			Resmi olmayan
			yargılar kayıt
			altına alınmaz.
			""",
			"""
			175/199

			Muhtemelen.
			""",
			"""
			176/199

			175. madde
			hakkında açıklama
			yapılmayacaktır.
			""",
			"""
			177/199

			Derin nefes alın.
			""",
			"""
			178/199

			Bu sağlık tavsiyesi
			değildir.
			""",
			"""
			179/199

			Sadece dramatik
			bir geçişti.
			""",
			"""
			180/199

			19 sayfa kaldı.

			Kaçış yok.
			""",
			"""
			181/199

			SON HÜKÜMLER

			Buraya kadar
			geldiniz.
			""",
			"""
			182/199

			Gerçekten
			etkileyici.
			""",
			"""
			183/199

			Ama işimiz henüz
			bitmedi.
			""",
			"""
			184/199

			183 maddeyi
			kabul ettiniz.

			Bunu da edin.
			""",
			"""
			185/199

			Şimdi bunu da
			kabul ettiniz.

			Güzel.
			""",
			"""
			186/199

			Ve bunu da.

			Devam.
			""",
			"""
			187/199

			Bu madde önceki
			kabulünüzü kabul
			ettiğinizi kabul
			eder.
			""",
			"""
			188/199

			Bu madde 187'yi
			kabul ettiğinizi
			tekrar kabul eder.
			""",
			"""
			189/199

			Risklerin hepsini
			anladınız mı?
			""",
			"""
			190/199

			Hayır cevabı
			beklenmektedir.
			""",
			"""
			191/199

			Biz de hepsini
			açıklamadık.
			""",
			"""
			192/199

			Bu bilinçli bir
			tercihtir.
			""",
			"""
			193/199

			Spoiler vermiyoruz.

			Ne kadar
			düşünceliyiz.
			""",
			"""
			194/199

			Riskleri bilmeden
			kabul ettiğinizi
			kabul ediyorsunuz.
			""",
			"""
			195/199

			Bunu garip
			bulabilirsiniz.

			Doğru.
			""",
			"""
			196/199

			Devam etmek
			istiyorsunuz.

			Belli oluyor.
			""",
			"""
			197/199

			Son üç kabul.

			Dayanın.
			""",
			"""
			198/199

			CİDDEN OKUDUNUZ MU?

			198 sayfayı gerçekten
			okuduğunuza
			inanmıyoruz.

			Büyük ihtimalle sadece
			butona basıyorsunuz.
			""",
			"""
			199/199 - SON

			Önceki 198 ve bu
			maddeyi okuduğunuzu
			oyundaki bütün
			küfürleri ve bütün
			olayları kabul
			edersiniz.

			Yapımcıya yapılan
			bütün küfürler direk
			size yansıtılacaktır.
			"""
		};
	}

	private static void clearTestBook(ServerPlayer player, String token) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (isTestBook(stack, token)) {
				player.getInventory().setItem(slot, ItemStack.EMPTY);
			}
		}
	}

	private static int findSafeFreeSlot(ServerPlayer player, int selectedSlot) {
		for (int slot = 0; slot < 36; slot++) {
			if (slot != selectedSlot && player.getInventory().getItem(slot).isEmpty()) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean prepareSelectedBookSlot(ServerPlayer player, TestBookSession session) {
		ItemStack selectedStack = player.getInventory().getItem(session.selectedSlot);
		if (selectedStack.isEmpty() || isTestBook(selectedStack, session.token)) {
			return true;
		}

		int freeSlot = findSafeFreeSlot(player, session.selectedSlot);
		if (freeSlot < 0) {
			return false;
		}

		player.getInventory().setItem(freeSlot, selectedStack.copy());
		player.getInventory().setItem(session.selectedSlot, ItemStack.EMPTY);
		if (session.movedSlot < 0) {
			session.movedSlot = freeSlot;
		}
		return true;
	}

	private static void restoreMovedItem(ServerPlayer player, TestBookSession session) {
		if (session.movedSlot < 0 || session.movedSlot >= player.getInventory().getContainerSize()) {
			return;
		}

		ItemStack movedStack = player.getInventory().getItem(session.movedSlot);
		if (movedStack.isEmpty() || !player.getInventory().getSelectedItem().isEmpty()) {
			return;
		}

		player.getInventory().setSelectedItem(movedStack.copy());
		player.getInventory().setItem(session.movedSlot, ItemStack.EMPTY);
	}

	private static boolean isTestBook(ItemStack stack, String token) {
		if (stack.isEmpty()) {
			return false;
		}

		CompoundTag probe = new CompoundTag();
		probe.putBoolean(TEST_BOOK_TAG, true);
		if (token != null && !token.isBlank()) {
			probe.putString(TEST_BOOK_TOKEN_TAG, token);
		}
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(probe);
	}

	private static Progress progressFromTags(ServerPlayer player) {
		for (String tag : player.getTags()) {
			if (!tag.startsWith(PROGRESS_TAG_PREFIX)) {
				continue;
			}

			String[] parts = tag.substring(PROGRESS_TAG_PREFIX.length()).split("_", 2);
			if (parts.length != 2) {
				continue;
			}

			try {
				int book = Math.max(1, Math.min(PAGE_COUNTS.length, Integer.parseInt(parts[0])));
				int nextPage = Math.max(1, Math.min(PAGE_COUNTS[book - 1], Integer.parseInt(parts[1])));
				return new Progress(book, nextPage);
			} catch (NumberFormatException ignored) {
				return new Progress(1, 1);
			}
		}
		return new Progress(1, 1);
	}

	private static void saveProgress(ServerPlayer player, int book, int nextPage) {
		clearProgressTags(player);
		player.addTag(PROGRESS_TAG_PREFIX + book + "_" + nextPage);
	}

	private static void clearProgressTags(ServerPlayer player) {
		for (String tag : List.copyOf(player.getTags())) {
			if (tag.startsWith(PROGRESS_TAG_PREFIX)) {
				player.removeTag(tag);
			}
		}
	}

	private record Progress(int book, int nextPage) {
	}

	private static final class TestBookSession {
		private final int serverId;
		private final int selectedSlot;
		private int movedSlot;
		private final String token;
		private WeakReference<ServerPlayer> activePlayer = new WeakReference<ServerPlayer>(null);
		private int book;
		private int nextPage;

		private TestBookSession(int serverId, int selectedSlot, int movedSlot, String token, int book, int nextPage) {
			this.serverId = serverId;
			this.selectedSlot = selectedSlot;
			this.movedSlot = movedSlot;
			this.token = token;
			this.book = book;
			this.nextPage = nextPage;
		}
	}
}
