# SweetCherry yayım hazırlık kontrol listesi

Bu belge ilk GitHub sürümünü hazırlamak ve aynı kontrolleri sonraki sürümlerde tekrarlamak içindir. PortableApps.com paketi ayrı bir hedef olarak ele alınır; ilk GitHub sürümünü engellemez.

## Önerilen ilk paket

İlk sürüm için en sade dağıtım, Maven'ın oluşturduğu `release/SweetCherry` klasörünün ZIP arşividir. Kullanıcı arşivi açar ve işletim sistemine uygun `run` scriptini çalıştırır. Bu paket Java içermez; JDK/JRE 17 veya üstü sistemde kurulu olmalıdır.

Paket adı örneği:

```text
SweetCherry-0.5.0.zip
```

Sürüm numarası kesinleştiğinde `pom.xml`, Git etiketi, ZIP adı ve sürüm notları aynı değeri kullanmalıdır. Yayımdan önce geliştirme sırasında anlamlı değişiklikler yapıldıysa `0.5.0` yerine yeni bir sürüm seçilebilir; bu karar kontrol listesini değiştirmez.

## Otomatik kontroller

GitHub Actions içindeki `CI` iş akışı hem Windows hem Linux üzerinde Java 17 ile test ve paketleme yapar. Yerel son kontrol:

```bat
mvnw.cmd clean package
release\SweetCherry\run.bat
```

Linux/macOS karşılığı:

```bash
sh ./mvnw clean package
sh ./release/SweetCherry/run.sh
```

`clean package`, testleri de çalıştırır. Bilinçli olarak testleri atlamak istenmedikçe yayıma hazırlanırken `-DskipTests` kullanılmamalıdır.

## Her sürümden önce

- [ ] `main` dalındaki CI Windows ve Linux üzerinde başarılı.
- [ ] `pom.xml` sürümü, Git etiketi ve paket adı aynı.
- [ ] Temiz bir GitHub kaynak ZIP'inden ilk derleme denenmiş.
- [ ] `release/SweetCherry` temiz bir klasöre kopyalanarak çalıştırılmış.
- [ ] Java bulunamadığında script anlaşılır hata veriyor.
- [ ] Giriş, Demo Database seçimi ve en az bir normal düğüm görüntüleme denenmiş.
- [ ] İlk açılışta `login-credentials.properties` üretilmiş; yeniden başlatınca aynı parolalar çalışmış ve kişisel parola dosyası ZIP'e eklenmemiş.
- [ ] Dört CTB export seçeneği ve download/delete akışı denenmiş.
- [ ] `user` hesabının yönetici uçlarına erişemediği doğrulanmış.
- [ ] Normal profilde yalnızca `health` ve `info`; operations profilinde beklenen Actuator/Swagger uçları doğrulanmış.
- [ ] Uygulamanın varsayılan olarak yalnızca `127.0.0.1` üzerinde dinlediği doğrulanmış.
- [ ] `myapp.log`, `allTenants`, `CTBDATA` ve `exportedFiles` yollarının paket klasörü altında kaldığı doğrulanmış.
- [ ] Alias/shared node/link/anchor içeren export sınırlaması sürüm notlarında açıkça belirtilmiş.
- [ ] Lisans dosyası ve Türkçe kurulum belgesi arşive eklenmiş.
- [ ] ZIP için SHA-256 özeti oluşturulmuş ve GitHub Release varlıklarına eklenmiş.

Windows SHA-256 örneği:

```powershell
Get-FileHash .\SweetCherry-0.5.0.zip -Algorithm SHA256
```

Linux/macOS örneği:

```bash
sha256sum SweetCherry-0.5.0.zip
```

## Sürüm notlarında mutlaka bulunması gerekenler

- SweetCherry'nin CherryTree yerine geçmediği ve CTB dosyalarıyla çalıştığı.
- Java 17+ gereksinimi.
- Varsayılan tarayıcı adresi, yerleşik hesap adları ve ilk açılışta parola dosyasının konumu.
- Önemli CTB dosyaları için işlem öncesinde yedek önerisi.
- Alias/link/anchor içeren exportların deneysel olduğu.
- macOS akışının gerçek cihazda henüz doğrulanmadığı (doğrulanana kadar).
- Bilinen önemli sorunlar ve ilgili belgelerin bağlantıları.

## GitHub üzerinde yayımlama sırası

1. Sürüm numarasını ve kullanıcıya dönük değişiklik listesini kesinleştir.
2. Temiz kaynak üzerinde bu listedeki kontrolleri tamamla.
3. `release/SweetCherry` klasörünü sürüm numaralı ZIP haline getir.
4. ZIP'in SHA-256 özetini üret.
5. İmzalı veya açıklamalı Git etiketi oluştur ve gönder.
6. GitHub Release taslağı oluştur; ZIP ve checksum dosyasını ekle.
7. Taslağı yayımlamadan önce temiz bir Windows makinede indirilen varlığı son kez dene.

İlk sürümden sonra bu süreç etiket tetiklemeli bir GitHub Actions iş akışına dönüştürülebilir. İlk kez yayımlarken elle oluşturulan taslak, paket içeriğini ve sürüm notlarını öğrenmek açısından daha güvenlidir.

## Yayımı engellemeyen fakat izlenecek işler

- Aynı adlı tenant yapılandırmasını güvenle değiştirme veya silme: aktif bağlantıyı kapatma, açık onay, yedek ve yeniden yükleme akışı. İlk sürümde dosya elle düzenlenir; yükleme mevcut dosyayı değiştirmez.
- Export parser'ında alias/shared node/link/anchor kimlik eşleme çalışması.
- macOS gerçek cihaz testi.
- PortableApps.com Format paketi.
- Otomatik güncelleme.
- Uzak erişim için ayrı güvenli dağıtım profili.
