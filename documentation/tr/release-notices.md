# 1.0.0 taslağına lisans ve atıf dosyalarını ekleme

Bu çalışma uygulamayı değiştirmez. JAR ve launcher mevcut `v1.0.0` paketinden alınır; SHA-256 değerlerinin aynı kaldığı kontrol edilir. Logo korunur. Lisans envanteri `distribution-notices/THIRD-PARTY-NOTICES.md` ve `licenses/` içindedir. Sonraki normal Maven paketlemeleri de bu dosyaları release klasörüne kopyalar.

1. Yamayı uygulayın, commit edin ve **main** dalına push edin. `v1.0.0` etiketini taşımayın.
2. GitHub release taslağında mevcut EXE, Windows ZIP, `SHA256SUMS.txt` ve `windows-bundle.json` bulunmalıdır.
3. Actions → **Complete 1.0.0 draft license notices** → Run workflow → **main** seçin.
4. Görev mevcut dosyaları GitHub sunucusunda indirir, checksum doğrular, lisans dosyalarını ekler ve Inno Setup ile installer'ı tekrar paketler. Java, JAR ve launcher yeniden derlenmez; Maven veya uygulama testleri çalıştırılmaz.
5. Eski dört dosya, değiştirilmeden önce 30 günlük kurtarma artifact'i olarak saklanır. Hazır yeni dosyalar da artifact olarak saklanır. Ardından taslak dosyaları güncellenir. Yayın otomatik yapılmaz.
6. Görev başarılı olunca taslakta EXE, Windows ZIP, `SHA256SUMS.txt`, `windows-bundle.json` ve küçük `SweetCherry-1.0.0-notices.zip` bulunduğunu kontrol edip yayınlayın.

**Büyük dosyaları kendi bilgisayarınızdan yeniden yüklemeniz gerekmez.** İşlem GitHub sunucusunda gerçekleşir. Önceden indirilmiş paketler yeni lisans dosyalarını içermez; yeni checksum'lar güncellenmiş paketlere aittir. Manifest'in uygulama commit'i aynı kalır; `noticesCommit` atıfları ekleyen paketleme revision'ını gösterir.

Yükleme kısmen başarısız olursa taslağı yayınlamayın; kurtarma artifact'inden eski dosyalar geri alınabilir veya görev yeniden çalıştırılabilir. Görev yalnız yayımlanmamış 1.0.0 taslağını kabul eder. Windows görevi bu değişiklik hazırlanan ortamda çalıştırılmamıştır; ilk gerçek çalıştırma GitHub Actions'ta yapılır.

Release açıklamasına eklenecek İngilizce paragraf:

> License and attribution documents are included in the Windows packages and are also available separately in SweetCherry-1.0.0-notices.zip. Application binaries were built from v1.0.0; the documentation-only packaging revision is recorded as noticesCommit in windows-bundle.json.
