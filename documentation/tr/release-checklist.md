# SweetCherry 1.0.0 yayımlama adımları

Bu aşama yeni özellik veya yeni test turu içermez. Kullanıcı son kaynak üzerinde **334 başarılı test** ve başarılı **Windows x64 bundled distribution** çalıştırması bildirdi. Bu kayıt, henüz oluşturulmamış 1.0.0 paketinin derlendiği anlamına gelmez; mevcut başarılı paket 0.5.0-SNAPSHOT adını taşımaktadır.

## Hazırlık

1. Verilen `SweetCherry-comment-removal-log-backup-2026-10-08.md` dosyasını yerelinizde saklayın. Yama geçici yorum arşivini depodan kaldırır.
2. Yayın yamasını uygulayıp `git diff --check` çalıştırın; değişiklikleri commit/push edin. Proje sürümü 1.0.0 olur; İngilizce kılavuz dağıtım klasörüne de kopyalanır.
3. Lisans/atıf kaydındaki açık konuyu aşağıdan okuyun. Bu hazırlık onu tamamlanmış olarak işaretlemez.
4. Yayın commit'i üzerinde etiketi oluşturup gönderin:

```bash
git tag -a v1.0.0 -m "SweetCherry 1.0.0"
git push origin v1.0.0
```

Önceden yayımlanmış bir etiketi veya sürümü yeniden kullanmayın.

## Son paket

GitHub Actions → **Windows x64 bundled distribution** → Run workflow ile **v1.0.0** referansından çalıştırın. Arayüzde etiket seçilemiyorsa GitHub CLI alternatifi:

```bash
gh workflow run windows-bundle.yml --ref v1.0.0
```

Bu işlem mevcut paketleme ve mevcut testleri yürütür; yeni test eklenmemiştir. Tag push'u kendiliğinden GitHub Release yayımlamaz.

Başarılı çalışmanın `SweetCherry-windows-x64-java17` artifact'ini indirip açın. Dış artifact ZIP'inin tamamını Release'e yüklemek yerine şu **dört dosyayı** alın:

| Artifact içindeki yol | Release asset'i |
| --- | --- |
| `dist/windows/SweetCherry-1.0.0-windows-x64-setup.exe` | Aynı dosya adı |
| `dist/windows/SweetCherry-1.0.0-windows-x64.zip` | Aynı dosya adı |
| `dist/windows/SHA256SUMS.txt` | Aynı dosya adı |
| `release/SweetCherry/windows-bundle.json` | Aynı dosya adı |

Manifest konumunu artifact içinde dosya adına göre de bulabilirsiniz. Dosya adlarında 0.5.0-SNAPSHOT varsa eski artifact kullanılmıştır. Checksum dosyası EXE ve portable ZIP içindir; EXE'nin dijital olarak imzalandığını göstermez. Kişisel parola dosyaları, günlükler veya kişisel CTB dosyaları eklemeyin. GitHub kaynak ZIP/tar arşivlerini kendisi sunar. Ayrı bir JAR tek başına yapılandırma ve demo klasörlerini içermediği için önerilen indirme değildir.

## GitHub Release

1. Repository → Releases → Draft a new release.
2. Mevcut **v1.0.0** etiketini seçin. Başlık: **SweetCherry 1.0.0**.
3. `documentation/releases/v1.0.0.md` içeriğini açıklamaya yapıştırın.
4. Yukarıdaki dört asset'i ekleyin. İlk yayın kararlı 1.0.0 olarak seçildiğinden prerelease işaretlemeyin; Latest olarak yayımlayabilirsiniz.
5. Açıklama ve dosya adları doğru olduğunda Publish release seçin.
6. Release yayımlandıktan sonra LinkedIn metnini paylaşın. Etiketli kurulum belgesi bağlantıları yayın commit'ine sabitlenmiştir.

## Kalan lisans/atıf kaydı

[Üçüncü taraf lisans planı](third-party-notices-plan.md) henüz tamamlanmış envanter değildir. Maven bağımlılıkları kadar kopyalanmış JavaScript/CSS, fontlar ve CherryTree ikonları da kapsamdadır. Gerekli lisans/NOTICE/atıf metinlerinin dağıtımda bulunması yayın hazırlığının açık maddesidir; başarılı test veya paketleme bunu doğrulamaz. Mevcut lisans dosyalarını ve runtime'ın lisans klasörlerini koruyun. Bu belgede lisans incelemesi tamamlandı iddiası yoktur.

## Yayın sonrasına bırakılanlar

Yeni geliştirme/test turları; bağımlılık bakımının sonraki adımları; ek editör konforu; görünür metinde arama; macOS gerçek cihaz doğrulaması; PortableApps paketi ve otomatik güncelleme. Mevcut uzak erişim kurulumu için [ağ rehberi](network-access.md) ve [HTTPS/oturum belgesi](https-and-session-security.md) kullanılır. HTTP, proxy ve firewall ayarları her kurulumun kendi sorumluluğundadır.
