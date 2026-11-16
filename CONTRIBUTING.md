# Katkıda bulunma

Minecraft Hell Mode'a katkı göndermeden önce mevcut issue ve pull request'leri kontrol edin. Küçük, tek amaca odaklanan değişiklikler incelemeyi ve geri dönük uyumluluk kontrolünü kolaylaştırır.

## Geliştirme ortamı

- Java 21 veya üzeri
- Minecraft 1.21.11 geliştirme bağımlılıklarını indirebilen internet bağlantısı
- Git ve proje içindeki Gradle Wrapper

Windows'ta `./gradlew.bat`, Linux/macOS'ta `./gradlew` kullanın. Global Gradle kurulumu gerekmez.

## Değişiklik akışı

1. Issue açarak büyük davranış değişikliklerini önce tartışın.
2. `main` branch'ini değiştirmeden ayrı bir branch oluşturun.
3. Kaynak kodu, gerekli kaynak dosyalarını ve regresyon testlerini aynı değişiklikte tutun.
4. Oynanış davranışını değiştiriyorsanız README veya ilgili dokümana etkisini yazın.
5. Aşağıdaki doğrulamaları çalıştırın:

   ```powershell
   ./gradlew.bat clean build
   ./gradlew.bat test
   ./gradlew.bat -I tools/bug-audit.init.gradle runServer
   ```

6. Pull request açıklamasında gerçek kullanıcı etkisini, geri dönük uyumluluk riskini ve test sonuçlarını belirtin.

## Kod kuralları

- Minecraft/Fabric sürümlerini `gradle.properties` içinde tek bir yerden güncelleyin.
- Sunucu tarafı kuralları istemci koduna taşımayın; istemci mixin'lerini `src/client` altında tutun.
- Yeni mixin ekliyorsanız yapılandırmaya ekleyin ve sunucu/client ortamını doğru seçin.
- Her tick çalışan kodda gereksiz chunk yükleme, sınırsız arama ve tekrar eden entity taraması yapmayın.
- Kullanıcı verisini silen veya kalıcı oynanış değişikliği yapan davranışlar için açık bir açıklama ve test ekleyin.
- Sahte yıldız, katkıcı, issue, benchmark veya test sonucu eklemeyin.

## Commit ve pull request

Commit mesajları değişikliğin amacını kısa ve açık biçimde anlatsın. Bir pull request tek bir ana hedef taşımalı; bağımsız temizlikleri ayrı değişikliklere bölmek tercih edilir.

CI; Gradle build'i, JUnit testlerini, JSON kaynak doğrulamasını ve Fabric sunucu audit'ini çalıştırır. CI başarısızsa önce `run/logs/latest.log` ve Gradle hata çıktısını inceleyin.

## Hata bildirme

Güvenilir bir hata raporu için Minecraft/Fabric/mod sürümlerini, işletim sistemini, yeniden üretme adımlarını, beklenen ve gerçekleşen sonucu, ilgili log bölümünü ve mümkünse yeni bir dünyada tekrar edilip edilmediğini ekleyin. Güvenlik açıklarını public issue olarak paylaşmayın; uygun özel bildirim kanalı için repository yöneticisine ulaşın.
