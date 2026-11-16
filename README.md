# Minecraft Hell Mode

[![Build](https://github.com/GeceKusu-TR/Minecraft-HELL-MOD/actions/workflows/build.yml/badge.svg)](https://github.com/GeceKusu-TR/Minecraft-HELL-MOD/actions/workflows/build.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/GeceKusu-TR/Minecraft-HELL-MOD/badge)](https://scorecard.dev/viewer/?uri=github.com/GeceKusu-TR/Minecraft-HELL-MOD)

Minecraft 1.21.11 için Fabric modudur. Mod; hayatta kalma deneyimini zorlaştıran oyun kuralları, mob davranışları, çevre tehlikeleri ve End ilerleme kuralları ekler. Değişiklikler sunucu tarafında uygulanır; istemci tarafı yalnızca HUD, ekran ve görsel bildirimleri sağlar.

## Uyumluluk

- Minecraft `1.21.11`
- Java `21` veya üzeri
- Fabric Loader `0.19.3` veya üzeri
- Fabric API `0.141.6+1.21.11` veya üzeri
- Fabric mod yükleyicisi kullanan istemci ve sunucu

İstemci ve sunucuda aynı Minecraft, Fabric Loader, Fabric API ve mod sürümlerini kullanın. Daha eski Minecraft sürümleri bu proje tarafından desteklenmez.

## Kurulum

### Oyuncu istemcisi

1. Minecraft `1.21.11` için Fabric Loader kurun.
2. Aynı sürüm için Fabric API jar dosyasını `.minecraft/mods/` klasörüne ekleyin.
3. Projenin yayınlanan `minecraftmod-<sürüm>.jar` dosyasını aynı klasöre ekleyin. `-sources.jar` dosyasını kurmayın.
4. Fabric profilini başlatın.

### Dedicated server

1. Minecraft `1.21.11` için Fabric server kurulumunu tamamlayın.
2. Fabric API ve mod jar dosyalarını sunucunun `mods/` klasörüne kopyalayın.
3. İlk çalıştırmada oluşan `eula.txt` dosyasını Mojang EULA'sını kabul ediyorsanız `eula=true` yapın.
4. Sunucuyu çalıştırın ve istemcilerin de aynı mod sürümünü kullandığını doğrulayın.

Mod, oynanış kurallarını sunucuda yürüttüğü için çok oyunculu sunucuda modun sunucuya kurulması zorunludur. İstemciye de kurulum yapılması HUD ve ekran bildirimleri için gereklidir.

## Geliştirme ve doğrulama

Windows:

```powershell
./gradlew.bat clean build
./gradlew.bat test
./gradlew.bat -I tools/bug-audit.init.gradle runServer
```

Linux/macOS:

```bash
./gradlew clean build
./gradlew test
./gradlew -I tools/bug-audit.init.gradle runServer
```

`build` derleme, kaynak jar ve remapped mod jar üretir. `test`, ölçekleme ve performans sınırı gibi saf kural regresyonlarını çalıştırır. `runServer` komutu Fabric sunucusunu audit moduyla başlatır; mixin hedeflerini yükler, kritik kural assertions'larını çalıştırır ve `build/audit/success.txt` işaretleyicisi üretir. Bu komut bir oyun dünyası açmak veya gerçek bir sunucu yayınlamak için değildir.

Başarılı mod jar dosyası `build/libs/minecraftmod-<sürüm>.jar` konumundadır.

## Proje yapısı

- `src/main/java`: ortak ve sunucu tarafı mod kodu
- `src/client/java`: istemciye özel kod ve mixin'ler
- `src/main/resources`: mod metadata'sı, mixin yapılandırmaları, texture'lar ve datapack kaynakları
- `src/test/java`: hızlı birim regresyon testleri
- `src/audit`: Fabric sunucu başlatma ve mixin yükleme audit'i
- `tools/bug-audit.init.gradle`: audit modunu `runServer` görevine bağlayan yapılandırma
- `.github/workflows/build.yml`: push ve pull request için otomatik build, test ve audit

## Sorun giderme

- `UnsupportedClassVersionError` görürseniz `java -version` çıktısının 21 veya üzeri olduğunu kontrol edin.
- `Incompatible mod set` veya eksik dependency hatalarında Minecraft, Fabric Loader, Fabric API ve mod sürümlerini birlikte kontrol edin.
- Mixin yükleme hatasında eski mod jar'larını `mods/` klasöründen kaldırın; aynı klasörde hem normal jar hem de `-sources.jar` bırakmayın.
- Build bozulduysa `./gradlew.bat clean build` ile yeniden deneyin. Ayrıntılı log `run/logs/latest.log` altında bulunur.
- Dedicated server'da istemci bağlanamıyorsa iki tarafta da Minecraft sürümünün ve mod jar checksum'ının aynı olduğunu doğrulayın.
- Audit sırasında hata oluşursa önce `run/logs/latest.log` ve `build/audit/` altındaki çıktıları inceleyin. Audit'in başarılı olması, her oynanış senaryosunun manuel olarak test edildiği anlamına gelmez.

## Katkı

Kod veya dokümantasyon katkısı göndermeden önce [CONTRIBUTING.md](CONTRIBUTING.md) dosyasını okuyun. Hata bildirimleri ve özellik önerileri için GitHub issue şablonlarını kullanın.

## Lisans

Bu proje [CC0 1.0](LICENSE) altında yayımlanır.
