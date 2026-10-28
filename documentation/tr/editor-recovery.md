# Editör kurtarma taslakları / Editor recovery drafts

## Türkçe

Düz metin ve zengin metin editörleri, kaydedilmemiş içeriği varsayılan olarak **5 saniyede bir bu tarayıcıdaki IndexedDB deposuna** yazar. Veritabanına otomatik kayıt yapılmaz. Sunucu kapalıyken de çalışır; oturumu açık tutmak için sunucuya periyodik istek göndermez. Editörün üstündeki **Kurtarma taslakları** panelinden kapatılabilir veya aralık 1–300 saniye olarak değiştirilebilir. Tercih aynı tarayıcı profilinde hatırlanır. Bu işlem sayfayı yenilemediği için undo/redo geçmişi korunur.

Taslak; metni, biçimlendirmeyi, bekleyen resim/dosya eklemelerini, tablo/kod kutusu düzenlemelerini, nesne silme seçimini ve undo/redo geçmişini içerir. Başka sekmede veritabanı değiştirilmesi bu editörün verisini yeni veritabanına göndermez: mevcut `_tenantView` kontrolü korunur. Her açık editörün taslak kaydı ayrıdır. Kayıt anahtarı kullanıcı, uygulamanın çalışma klasörü, veri kaynağı adı, JDBC adresi ve editör/düğüm yoluna bağlıdır; bağlantı adresi ve kimlik bilgileri sayfaya verilmez. Çalışma klasörü veya JDBC adresi değişirse eski taslak yeni kaynak altında görünmez.

**Kaydet** ve **Kaydet ve devam et**, JavaScript açıkken mevcut kayıt servisini çağırır ve açık bir JSON başarı yanıtı bekler. Oturum kapanması, kaynak değişimi, doğrulama hatası, ağ kesilmesi veya sunucunun durması halinde editörden ayrılmaz; yazı ve geçmiş bellekte kalır. İşlem sırasında form geçici olarak kilitlenir. Başarı halinde mevcut yönlendirme davranışı korunur; **elle Kaydet ve devam et hâlâ sayfayı yeniden yükler ve geçmişi sıfırlar**. Otomatik kurtarma taslağı bu düğmeyi kullanmaz. JavaScript yoksa eski form gönderimi sürer ve kurtarma koruması çalışmaz.

### Kurtarma

1. Başarısız kayıt sonrası eski sekmeyi açık tutun. Gerekirse **Mevcut taslağı indir** ile JSON kurtarma kopyası alın.
2. Yeni sekmede giriş yapın; önceki veri kaynağını seçip aynı düğümün aynı editörünü açın.
3. Kurtarma panelinde tarih/saatle listelenen taslağı seçip **Geri yükle** düğmesine basın. İçeriği kontrol edin; ardından **Kaydet** seçin.
4. Veritabanındaki başlangıç içeriği farklıysa otomatik geri yükleme engellenir. Taslağı JSON olarak indirip metni elle karşılaştırın. JSON dosyası CTB değildir ve otomatik içe alma arayüzü yoktur; düz metinde `payload.text`, zengin metinde `payload.state.runs` metni içerir. Resimler/dosyalar ilgili pending alanlarında tutulur.

Zengin metinde mevcut revision (metin/nesne durumu) eşleşmelidir; düz metinde başlangıç metni aynı olmalıdır. Bir HTTP zaman aşımı kaydın kesin başarısız olduğunu kanıtlamaz: sunucuda tamamlanmış olabilir. Yeniden kaydetmeden önce yeni sekmede sonucu kontrol edin. Başarılı kayıt yalnız bu sekmenin ve bu sekmede geri yüklenen taslağın kopyasını siler; başka açık editörlerin taslaklarını silmez.

### Sınırlar

- Taslaklar **şifrelenmeden tarayıcı profilinde** tutulur; ortak bilgisayarda otomatik korumayı kapatın ve mevcut taslakları silin. Çıkış yapmak tek başına taslakları silmez. Sunucu izinleri ve kullanıcıya özgü listeleme korunur; bu, aynı işletim sistemi/tarayıcı profilini kullanan kişilere karşı şifreleme değildir.
- Tek taslak için yaklaşık **32 MiB** sınır vardır (JSON UTF-16 uzunluğu üzerinden ihtiyatlı hesap). Resimler ve uzun undo geçmişi bu sınıra dahildir. Kota/özel gezinme/depolama hatasında görünür uyarı çıkar; mevcut taslağı indirerek koruyun. Tarayıcı depolaması yedek değildir; tarayıcı temizliği/cihaz kaybı/sayfanın son aralıktan önce kapanması veri kaybettirebilir.
- Taslaklar 7 gün sonra kurtarma listesinde gösterilmez; sonraki editör açılışında süresi dolan kayıtlar temizlenir. İstenirse panelden tek tek silinebilir. Silme editördeki yazıyı silmez; otomatik koruma açık ve içerik değiştirilmişse tekrar taslak oluşabilir.
- Aynı origin (protokol/host/port) ve aynı tarayıcı profili gerekir. `localhost`, IP, HTTPS ve farklı portlar birbirinin taslaklarını görmez. Sunucu kapanmadan önce son periyodik taslağı alınmış içerik korunur; pagehide/arka plana geçiş ek kayıt denemesi yapar ama aniden kapanmayı garanti edemez.

### Manuel kontroller

- Her iki editörde metin yazın; taslak kaydedildi mesajını bekleyin. Sunucuyu durdurun ve Kaydet seçin: sayfa kalmalı; mevcut taslak indirilebilmeli.
- Sunucuyu açıp yeni sekmede aynı kaynak/düğüm/editöre dönün. Taslağı geri yükleyip undo/redo ve kayıt deneyin.
- Başka sekmede farklı kaynak seçin; eski editörde Kaydet seçin. Yeni kaynağa yazılmamalı ve eski içerik kaybolmamalı.
- Oturumu kapatıp eski sekmeden kaydetmeyi deneyin; tekrar giriş ve geri yüklemeyi kontrol edin.
- Resim, dosya, tablo ve kod kutusu ekleyip/değiştirip bir nesneyi silin; taslak geri yüklemesinde görünümler, geri alma ve kayıt çalışmalı.
- Başka sekmeden aynı düğümü gerçekten değiştirin; eski taslağın otomatik geri yüklenmesi engellenmeli, indirme çalışmalı.
- Otomatik korumayı kapatıp açın, aralığı değiştirin; yeni sekmede tercih hatırlanmalı. Farklı kullanıcı/kaynak/düğüm aynı taslakları görmemeli.

## English

Both editors save a **browser-local recovery draft every 5 seconds** by default. Expand **Recovery drafts** to disable it or choose 1–300 seconds. It uses IndexedDB, works while the server is offline, sends no keep-alive requests and never automatically writes to a database. Undo/redo, rich formatting, staged objects and pending binary uploads are included. Manual **Save and continue** still reloads the page; periodic recovery drafts do not.

With JavaScript enabled, a failed/expired/stale/offline save keeps the editor open. Sign in in a new tab, select the original source, open the same editor and choose a dated draft to restore. Check it before saving. A different initial text/rich-text revision blocks automatic restoration; download the JSON to recover text manually. There is no JSON upload/import interface. A timeout may occur after a successful database commit: check before retrying. Existing role, CSRF, source-token, revision and read-only checks remain in force.

Records are separated by authenticated user, application working directory, source name/JDBC URL, editor path and tab. They are unencrypted in the browser profile, expire after seven days and are limited to approximately 32 MiB each including history and staged uploads. Existing drafts are not deleted by logout or disabling automatic drafts. Discard them on shared computers. A successful save removes only this tab's draft and the draft it restored. Browser quotas, private browsing, clearing site data, different origins/profiles and sudden shutdown before the next interval can prevent recovery; a visible storage warning and **Download current draft** provide a fallback. The download is recovery JSON, not a CTB or an automatic backup.
