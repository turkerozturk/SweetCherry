# Bağlı düğüm içeriklerini yerinde gösterme

**View Mode → Bağlı düğüm içeriklerini göster** seçeneği içerik zoom kontrollerinin
yanındadır. Varsayılan normal bağlantı görünümüdür. Seçim tarayıcıda hatırlanır;
düğümler arasında gezinirken ve sayfa yenilenince korunur, diğer sekmelere de
yansır. Tarayıcı depolaması kullanılamıyorsa yalnız açık sayfa boyunca geçerlidir.

Örneğin ortak kurulum adımını ve ekran görüntülerini tek bir düğümde tutup farklı
kılavuzlardan bu düğüme bağlantı verin. Bu görünümde bağlantının yerinde hedef
düğümün içeriği gösterilir. Üstteki küçük **Kaynağı aç** bağlantısı asıl düğümü
yeni sekmede açar; düzenleme o düğümün normal editöründen yapılır. Seçenek tekrar
kapatıldığında özgün bağlantılar ve yerleşim geri gelir. Kaynak veya kılavuzun
metni, etiketleri, tarihleri, bağlantıları ve CTB şeması değiştirilmez.

Ağaç görünümü, mobil okuyucu ve normal düğüm okuyucusundaki rich text içeriklerde
çalışır. Hedefin resim, tablo ve codebox içeriği mevcut gösterim hattıyla korunur.
Düz metin, Markdown, temizlenmiş HTML ve kod düğümleri de hedef olabilir; bunların
içindeki bağlantılar ayrıca açılmaz. Yalnız rich text içindeki düğüm bağlantıları
iç içe genişletilir. Web/dosya/indirme bağlantıları normal davranır.

Çapalı bir düğüm bağlantısı, çapa sonrasındaki bölümü değil hedef düğümün tamamını
gösterir. Aynı düğümün kendi çapaları normal gezinme bağlantısı olarak kalır.
Kopyalanan çapaların kimlikleri her parça için ayrılaştırılır; aynı parçanın
iki kere kullanılması veya kaynak düğümdeki çapalar birbirine karışmaz.

## Güncelleme ve sınırlar

Kaynak düğümdeki değişiklik, kılavuz yeniden açıldığında/yenilendiğinde veya
seçenek kapatılıp tekrar açıldığında gelir. Başka sekmede düzenlemeyi anlık
izleyen senkronizasyon yoktur. Aynı hedefin tekrarları yalnız açık kılavuzun
istek önbelleğini paylaşır; içerikler tarayıcı depolamasına kaydedilmez.

Aynı içeriğe dönen bağlantılar, paylaşılan düğüm ID'leri farklı olsa da döngü
olarak algılanır. Eksik/bozuk hedefler, REMINDERS/audit/çöp kutusu dalları ve
sınırı aşan içerikler küçük açıklamayla normal bağlantı olarak kalır. Form,
script, iframe ve uzaktaki resimler parçaya alınmaz. Yarım kalan istek, görünüm
kapatıldıktan veya başka düğüme geçildikten sonra eski içeriği ekleyemez.

Mevcut `myapp` bölümüne birleştirip değişiklikten sonra yeniden başlatın:

```yaml
myapp:
  node-fragments:
    max-depth: 3
    max-inclusions: 20
    max-source-characters: 250000
    max-html-characters: 1048576
    max-object-bytes: 16777216
    max-objects: 200
```

Derinlik 1–5, açık düğüm başına parça sınırı 1–50 olabilir; tekrarlar ayrı sayılır.
Kaynak karakter bütçesi (1–1000000) düğüm, tablo ve codebox metinlerini kapsar.
Tek HTML yanıtı 1024–2097152 karakter; sayfadaki eklemelerin toplam HTML bütçesi
sabit 4.194.304 karakterdir. Her hedefteki resim/ek dosya toplam boyutu
1024–33554432 byte; gömülü nesne sayısı 1–500 aralığındadır. Boyutlar içerik ve
BLOB yüklenmeden önce meta bilgilerden denetlenir. İstek 10 saniyede zaman aşar.

USER ve salt okunur bağlantılar kullanabilir. Güncel tenant belirteci zorunludur;
API yalnız GET ve `no-store` kullanır. Veritabanı değişince eski sekmenin isteği
reddedilir. Bu düğme bir tarayıcı okuma tercihidir. PDF için bağımsız
[içerik ve yerleşim ayarları](pdf-content-options.md) vardır. Editör, diğer export
çıktıları, mind map ve saklanan bağlantılar normal biçimde kalır.

## Doğrulama

```bat
mvnw.cmd -Dtest=NodeFragmentServiceTest,NodeFragmentTemplateTest,NodeFragmentSecurityTest,ReminderTemplateTest test
mvnw.cmd clean package
```

Manuel: aynı biçimli paragrafta iki bağlantı, liste ve tablo hücresi; ekran
resimli/tablo/codebox içeren hedef; aynı parçanın iki kere kullanımı ve çapaları;
paylaşılan düğüm ve A → B → A döngüsü; eksik/büyük hedef deneyin. Kapatınca özgün
biçim ve bağlantıların dönmesini, her iki okuyucuda gezinirken tercihin
korunmasını ve resmin mouse/klavye ile açılmasını kontrol edin. Kaynağı düzenleyip
kılavuzu yeniden açın; istek beklerken veri kaynağını değiştirin.

## Kaynak etiketi olmadan satır içi değerler

Bağlı içerikler artık bağlantının bulunduğu konumdan başlar. Kaynak görünürken **Kaynağı aç: düğüm adı** bağlantısı aynı konumdadır; ardından satır sonu ve hedef içerik gelir.

**View Mode → Bağlı içerik kaynaklarını gizle**, varsayılanı kapalı olan bağımsız ikinci tercihtir. Bağlı içerik görünümü açıkken etkinleştirilirse kaynak bağlantısı ve ona ait satır sonu gizlenir. Hedef metin iç bağlantının bulunduğu yerden başlar; başına/sonuna yapay satır sonu eklenmez. Örneğin hedefi `sweetcherry_1.11.0_all_os_portable.zip` olan bağlantı doğrudan `Get-FileHash -Algorithm SHA256 .\` metninden sonra kullanılabilir.

Hedefin gerçek satır sonları, boş satırları, biçimlendirmesi ve anchor ad alanları korunur. Tablo, liste ve gömülü codebox kaydırma alanları yapısal yerleşimlerini korur. Bu yalnız tarayıcı okuma görünümünü değiştirir; kayıtlı düğümler ve PDF ayarları bağımsızdır. İki tercih de gezinme/yenileme boyunca hatırlanır ve sekmeler arasında eşitlenir. Kaynak gizleme tercihi bağlı içerikler kapalıyken de seçilebilir ve eklenmiş hedefleri tekrar indirmez.
