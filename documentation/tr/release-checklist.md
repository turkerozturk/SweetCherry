# SweetCherry 1.2.0 yayımlama adımları

1.1.0 sonrası biriken uyumlu yeni özellikler ve düzeltmeler için yeni sürüm **1.2.0**, etiket
**v1.2.0** olacaktır. Son özellik commit’i `ad05f1269356a7de163b32805f0d1f0a57874cef` üzerinde
kullanıcı **471 başarılı Java testi**, manuel taşıma kontrolleri ve başarılı Windows workflow’u
bildirdi. Etiket, aşağıdaki sürüm hazırlığı commit’ine konur; eski özellik commit’ine konmaz.

## 1. Sürüm hazırlığını uygula

Temiz main dalında:

```bat
git switch main
git pull --ff-only
git apply --check sweetcherry-ad05f12-release-1.2.0.patch
git apply sweetcherry-ad05f12-release-1.2.0.patch
git diff --check
mvnw.cmd clean package
```

Konsolda proje sürümü 1.2.0 olmalı, mevcut testler geçmeli. Bu yama yalnız proje sürümü ve
README/changelog/yayın belgelerini değiştirir; yeni uygulama özelliği veya bağımlılık güncellemesi içermez.
README’deki mevcut 1.1.0 ekran görüntüsü korunur; yeni ekran görüntüsü yayın şartı değildir.

IntelliJ’den bu değişiklikleri `Prepare SweetCherry 1.2.0 release` mesajıyla commit/push edin.
Cmd’de GitHub kimlik doğrulaması daha önce çalışmadığından aynı başarılı IntelliJ yöntemini kullanabilirsiniz.

## 2. Yeni commit’i etiketle ve etiketi gönder

Commit/push sonrasında:

```bat
git status --short
git log -1 --oneline
git tag -a v1.2.0 -m "SweetCherry 1.2.0"
```

`git status --short` boş olmalı. Son commit, sürüm hazırlığı commit’i olmalı. IntelliJ:
**Git araç penceresi → Tags → v1.2.0 → sağ tık → Push to origin**.

Cmd kimlik doğrulaması çalışıyorsa alternatif `git push origin v1.2.0` olur. Her ikisini yapmak gerekmez.
Var olan v1.0.0 ve v1.1.0 etiketlerine/paketlerine dokunmayın. Etiket göndermek kendi başına release yayımlamaz.

## 3. Etiketten Windows paketini üret

GitHub → **Actions → Windows x64 bundled distribution → Run workflow → v1.2.0 → Run workflow**.
Dal/etiket seçimi main değil **v1.2.0** olmalı. Önceden ad05f126/main üzerinde alınan paketi tekrar
kullanmayın: onun uygulama sürümü hâlâ 1.1.0 idi.

Görev testleri, Java 17 paketlemeyi ve system Java olmadan installer/launcher kontrollerini çalıştırır.
Başarılı olunca `SweetCherry-windows-x64-java17` artifact’ini indirin ve dış ZIP’i açın.
**Complete 1.0.0 draft license notices** workflow’unu çalıştırmayın; eski 1.0.0 taslağına özeldir.
Yeni normal paketleme lisans/atıfları zaten içerir.

## 4. Paketleri ayır ve son kontrolü yap

Dış artifact ZIP’i değil, içindeki şu beş dosya release’e eklenecek:

| Dosya | Amaç |
| --- | --- |
| `dist/windows/SweetCherry-1.2.0-windows-x64-setup.exe` | Windows kurulum sihirbazı |
| `dist/windows/SweetCherry-1.2.0-windows-x64.zip` | Portable uygulama |
| `dist/windows/SweetCherry-1.2.0-notices.zip` | Lisans ve atıflar |
| `dist/windows/SHA256SUMS.txt` | Üç uygulama/lisans dosyasının SHA-256 değerleri |
| `release/SweetCherry/windows-bundle.json` | Sürüm, commit ve Java bilgisi |

Manifest’te `applicationVersion` **1.2.0**, `commit` aşağıdaki etiket commit’i ile aynı olmalı:

```bat
git rev-parse "v1.2.0^{commit}"
```

Portable ZIP’i ayrı temiz klasöre açıp launcher, giriş, demo açma ve uygulamayı durdurmayı kısaca deneyin.
Bu son paket kontrolüdür; kapsamlı manuel testleri baştan tekrarlamak gerekmez. Paketler, kaynak kodundaki
471 testten ayrı olarak son etiket üzerinden doğrulanır. `THIRD-PARTY-NOTICES.md`, `licenses/`,
Java runtime ve iki hızlı başlangıç CTB’si pakette bulunmalı. SHA-256 değerleri eklenen dosyalarla eşleşmeli.
Kullanılmış kurulum klasörünüzü veya kendi CTB/ayar/parola dosyalarınızı ZIP’leyip yüklemeyin.

## 5. GitHub Release taslağını oluştur

GitHub → **Releases → Draft a new release**:

1. Tag: mevcut **v1.2.0**.
2. Title: **SweetCherry 1.2.0**.
3. Description: [documentation/releases/v1.2.0.md](../releases/v1.2.0.md) dosyasının Markdown içeriği.
4. Yukarıdaki beş dosyayı assets bölümüne yükleyin; yüklemelerin bitmesini bekleyin.
5. **Set as a pre-release** işaretli olmasın. **Set as the latest release** seçili olsun.
6. **Save draft** ile kontrol edebilirsiniz. Son kontrol tamamlanınca **Publish release**.

GitHub iki Source code arşivini otomatik ekler; toplamda yedi asset görülmesi normaldir.
Bu belgeler/komutlar release’i otomatik yayımlamaz; son Publish release işlemini arayüzden siz yaparsınız.

## 6. Yayın sonrası

README’nin `/releases/latest` bağlantısı yeni kararlı sürüme gider. 1.1.0 indirmelerini ve etiketini
koruyun. Yayımlanmış v1.2.0 etiketini taşımayın veya paketlerini sessizce başka derlemeyle değiştirmeyin;
yeni düzeltme gerekiyorsa 1.2.1 olur. Bir sonraki geliştirmede main sürümü isteğe bağlı olarak
`1.2.1-SNAPSHOT` yapılabilir; bunu şimdi yapmak yayın için şart değildir. Ayrı nightly kanalı gerekmiyor.
