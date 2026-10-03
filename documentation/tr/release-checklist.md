# SweetCherry yayım hazırlık kontrol listesi

Bu belge ilk GitHub sürümünü hazırlamak ve aynı kontrolleri sonraki sürümlerde tekrarlamak içindir. PortableApps.com paketi ayrı bir hedef olarak ele alınır; ilk GitHub sürümünü engellemez.

## Önerilen ilk paket

İlk sürüm için en sade dağıtım, Maven'ın oluşturduğu `release/SweetCherry` klasörünün ZIP arşividir. Kullanıcı arşivi açar ve işletim sistemine uygun `run` scriptini çalıştırır. Bu paket Java içermez; JDK/JRE 17 veya üstü sistemde kurulu olmalıdır.

Paket adı örneği:

```text
SweetCherry-0.5.0.zip
```

Sürüm numarası kesinleştiğinde `pom.xml`, Git etiketi, ZIP adı ve sürüm notları aynı değeri kullanmalıdır. Yayımdan önce geliştirme sırasında anlamlı değişiklikler yapıldıysa `0.5.0` yerine yeni bir sürüm seçilebilir; bu karar kontrol listesini değiştirmez.

## Son özellik turundan sonra odak

Yeni özellik turu tamamlandı: düz/rich text düzenleme, gömülü nesneler, dört yönlü taşıma, tek düğüm/alt ağaç çoğaltma ve bookmark yönetimi artık yayım adayının parçasıdır. Bundan sonraki öncelik yeni özellik değil, doğrulama ve paketlemedir.

- [ ] Bookmark shared kimlikleri, dar ekran kaldırma düğmesi ve gerçek/shared yolları son kez kontrol edildi.
- [ ] Footer yalnızca çalışan bağlantılar içeriyor; Pricing ve boş Features/FAQs bağlantıları yok.
- [ ] Help metinlerinin kapsamı gözden geçirildi; mevcut yardım bağlantısı korunuyor, içerik kararı ayrı.
- [ ] `location-time-language.md` içindeki konum/zaman/widget maddeleri tamamlandı veya sürüm kapsamı açıkça daraltıldı.
- [ ] Read-only CTB, user hesabı, CSRF, stale tenant ve güvenilmeyen CTB/HTML girdileri son güvenlik turundan geçti.
- [ ] Temiz paket Windows/Linux üzerinde başlatıldı; gerçek not arşivi yerine CTB kopyasında CherryTree ile karşılaştırıldı.
- [ ] Yeni editör ve yazma işlemleri sürüm notları/bilinen sınırlamalar bölümüne eklendi.

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
- [ ] CTB değiştirildikten sonra eski sekmenin düğüm bağlantısı ve silme/export formu yeni CTB üzerinde çalışmamış.
- [ ] Oturum süresi dolunca giriş ekranında açıklama görülmüş; yeniden girişten sonra CTB seçimi istenmiş.
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

- `exportedFiles` listesindeki her CTB için `CTBDATA` klasörüne uygun göreli yol içeren tenant `.txt` tanımı üretip indirme düğmesi eklemek. Aynı ada sahip mevcut tanımları değiştirmemeli.
- Dışa aktarılan dosya silindikten sonra başarı mesajından listeye geri dönme bağlantısı veya liste içinde sonuç gösterme akışı.
- Aynı adlı tenant yapılandırmasını güvenle değiştirme veya silme: aktif bağlantıyı kapatma, açık onay, yedek ve yeniden yükleme akışı. İlk sürümde dosya elle düzenlenir; yükleme mevcut dosyayı değiştirmez.
- HTML/PDF içeriği, grid ve XML işleyicilerinin güvenilmeyen CTB verisine karşı güvenlik incelemesi ve örnek kötü amaçlı CTB testleri.
- Export parser'ında alias/shared node/link/anchor kimlik eşleme çalışması.
- `plain-text` içindeki `{sweet-cherry}` işaretiyle açılan ayrı HTML gösterim ve zengin metin düzenleyici tasarımı. Bu işaret uygulanmadan önce güvenilmeyen HTML'nin arındırılması ve CherryTree ile uyumluluk ayrıca incelenmeli.
- macOS gerçek cihaz testi.
- PortableApps.com Format paketi.
- Otomatik güncelleme.
- Uzak erişim için ayrı güvenli dağıtım profili.
- Giriş sınırının çoklu kullanıcı, proxy yeniden başlatma ve uygulama yeniden başlatma davranışını değerlendirmek; sınırın uygulama belleğinde tutulduğunu ve yeniden başlatılınca sıfırlandığını belgelemek.

## İnternetten erişim için ayrı kabul kontrolü

İlk dağıtım yerel kullanım içindir. Dinamik DNS ve proxy üzerinden uzaktan erişim sağlamak, giriş ekranının ve seçilen CTB içeriğinin internete açılması anlamına gelir. Yalnızca TLS sertifikasının çalışması bu kontrolün yerine geçmez.

- [ ] İnternet yönlendiricisinde yalnızca proxy için gereken portlar açık; SweetCherry'nin 8080 ve ikinci HTTP portu internete doğrudan yönlendirilmemiş.
- [ ] SweetCherry host güvenlik duvarı yalnızca gereken LAN istemcilerine ve proxy hostuna izin veriyor; `server.address: 0.0.0.0` tek başına bir erişim kuralı değildir.
- [ ] Tarayıcı–proxy bağlantısı HTTPS; proxy–SweetCherry arasındaki HTTP trafiği güvenilir LAN/VPN içindedir.
- [ ] Oturum çerezi, proxy başlıkları, yönlendirme ve HTTPS davranışı aynı alan adı üzerinden doğrulandı. HTTP üzerinden localhost/LAN erişimi istendiğinde `Secure` çerezinin etkisi ayrıca kararlaştırıldı.
- [ ] Giriş denemesi sınırı gerçek istemciyi yanlış engellemiyor; farklı WAN istemcileri, doğrudan LAN ve localhost ayrı ayrı denendi.
- [ ] CTB içeriğinden HTML üreten yollar, dosya yükleme/dışa aktarma uçları, kullanıcı rolleri ve aktif operasyon profili ayrı ayrı gözden geçirildi.

### Giriş sınırı için güvenilir proxy adresi

Varsayılan davranış TCP bağlantısının IP adresini kullanır. Caddy arkasında bütün WAN istemcileri aynı proxy IP'sinden görünür. Caddy'ye gelen istemci adresini tek başlıkta iletmek için ilgili site bloğuna örneklerdeki IP adreslerini sizinkilerle değiştirerek şunu koyun: 

```caddyfile
xyz.duckdns.org {
    reverse_proxy 192.168.0.5:8080 {
        header_up X-SweetCherry-Client-IP {remote_host}
    }
}
```

SweetCherry'nin JAR dışındaki `application.yml` dosyasında, **uygulamanın doğrudan bağlantıda gördüğü Caddy IP'sini** belirtin:

```yaml
myapp:
  login:
    trusted-proxy-address: 192.168.0.2
```

Mevcut `myapp.login` bölümüne yalnızca `trusted-proxy-address` satırını ekleyin; diğer kullanıcı adlarını silmeyin. Caddy yapılandırmasını doğrulayıp yeniden yükledikten ve SweetCherry'yi yeniden başlattıktan sonra logdaki `clientIp` değerini kontrol edin. Proxy dışındaki isteklerde başlık yok sayılır. Güvenilir proxy eşleşse bile başlık eksik, birden fazla veya geçersizse doğrudan proxy IP'si kullanılır. Bu ayar yalnızca giriş sınırı ve giriş logları içindir; tüm uygulamada proxy başlıklarına güvenme ayarı değildir.

Docker ağ kipine göre `{remote_host}` gerçek WAN istemcisi yerine bir ağ geçidi adresi olabilir. Farklı dış ağlardan denemelerde aynı `clientIp` görülürse güven sınırını genişletmeyin; önce Caddy'nin gelen bağlantıda gördüğü adresi ve Docker ağ yolunu inceleyin. Uygulama yeniden başlatılırsa 15 dakikalık sayaçlar bellekte oldukları için sıfırlanır.
