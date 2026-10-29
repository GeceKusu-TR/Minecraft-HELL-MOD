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

## Proje yapısı

- `src/main/java`: Ortak ve sunucu tarafı mod kodu
- `src/client/java`: İstemciye özel kod ve mixin'ler
- `src/main/resources`: Mod metadata'sı, mixin yapılandırmaları, texture'lar ve datapack kaynakları
- `src/audit`: Ayrı hata denetimi için kullanılan test kaynakları
- `tools/bug-audit.init.gradle`: Hata denetimi çalıştırma yapılandırması

## Lisans

Bu proje [CC0 1.0](LICENSE) altında yayımlanır.
