# Minecraft Hell Mode

Minecraft 1.21.11 için Fabric mod projesi. Mod, hayatta kalma deneyimini daha zor ve tehlikeli hale getiren çeşitli oyun kuralları, mob davranışları ve çevre değişiklikleri ekler.

## Gereksinimler

- Minecraft `1.21.11`
- Java `21` veya üzeri
- Fabric Loader `0.19.3` veya üzeri
- Fabric API

## Geliştirme ortamını çalıştırma

Windows:

```powershell
./gradlew.bat runClient
```

Linux/macOS:

```bash
./gradlew runClient
```

## Derleme

Windows:

```powershell
./gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

Derlenen mod dosyaları `build/libs/` klasörüne yazılır.

## Geliştirme duyurusu

Bir sonraki büyük güncelleme şimdilik uzun bir süreliğine rafa kaldırılmıştır. Bunun sebebi stratejik bir plan, gizli bir geliştirme takvimi veya dünyayı sarsacak bir tasarım kararı değildir; aklıma fikir gelmedi. Gelen iki fikir de kapıda bekleyip içeri girmeden geri döndü.

Mod şu anki hâliyle oyuncuyu öldürmeye devam edecek. Geliştirici ise yeni fikir bulana kadar README dosyasına bakıp ciddi bir plan yapıyormuş gibi davranacaktır. Yeni güncelleme, fikirler tekrar çalışmaya karar verirse ve geliştirici kahvesini yanlışlıkla kod yerine içerse geri dönebilir.

## Proje yapısı

- `src/main/java`: Ortak ve sunucu tarafı mod kodu
- `src/client/java`: İstemciye özel kod ve mixin'ler
- `src/main/resources`: Mod metadata'sı, mixin yapılandırmaları, texture'lar ve datapack kaynakları
- `src/audit`: Ayrı hata denetimi için kullanılan test kaynakları
- `tools/bug-audit.init.gradle`: Hata denetimi çalıştırma yapılandırması

## Lisans

Bu proje [CC0 1.0](LICENSE) altında yayımlanır.
