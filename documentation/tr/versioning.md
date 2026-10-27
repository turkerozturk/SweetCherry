# Sürümleme ve release akışı

Uygulama sürümünün tek kaynağı `pom.xml` içindeki proje `<version>` alanıdır; Spring Boot parent sürümü değildir. Actuator sürüm bilgisi derleme metadatasından alınır; dış YAML içinde çözülmemiş `@project.version@` kalırsa bu metin sürüm olarak gösterilmez. Git bilgileri ayrıca `git.properties` dosyasına yazılır. JAR dışındaki YAML'de eski bir sabit `info.app.version` varsa kaldırın.

İlk yayın **1.0.0**, Git etiketi **v1.0.0** olarak hazırlanmıştır. Her commit için sürüm artırılmaz; derlemeler commit kimliğiyle ayrılır.

`MAJOR.MINOR.PATCH` yaklaşımı kullanılır:

- `1.0.1`: uyumlu hata düzeltmeleri.
- `1.1.0`: uyumlu yeni özellikler.
- `2.0.0`: belgelenmiş kullanım, tenant ayarları veya desteklenen CTB davranışlarında uyumsuz değişiklikler.

Bu numaralandırma bütün CherryTree özelliklerinin uygulandığı veya hatasızlık garantisi verildiği anlamına gelmez. Sınırlamalar sürüm notlarında açıklanır. Kaynak: https://semver.org/

Yayın hazırlığını commit/push edin, etiketi aynı commit'e koyun ve son dağıtımı o etiketten oluşturun. Yayımlanmış etiketi taşımayın; aynı sürümün dosyalarını farklı bir derlemeyle değiştirmeyin. Düzeltme yeni bir sürüm olur. 1.3.0; 1.2.0 sonrasındaki kod kutusu düzenleme, LaTeX gösterimi, gezinme, harita içeriği ve CTB export iyileştirmelerini kapsar. Yeni özellikler bulunduğu için yalnız düzeltme numarası olan 1.2.1 yerine 1.3.0 seçilmiştir. 1.3.0 yayımlandıktan sonra main üzerinde sıradaki çalışma sürümü isteğe bağlı `1.3.1-SNAPSHOT` olabilir; sonraki yayında kapsamına göre 1.3.1 veya 1.4.0 seçilir.

Adım adım işlem ve asset listesi: [yayın hazırlığı](release-checklist.md). 1.0.0 hazırlığındaki geçici yorum arşivi daha önce yedeklenip kaldırılmıştır; bu adım her release için tekrarlanmaz. Git geçmişi korunur.

## 1.1.0 ve sonrası çalışma

1.1.0 uyumlu yeni özellikleri ve hata düzeltmelerini birlikte içerir. Normal değişiklikleri main dalına commit/push etmeye devam edin; her commit için etiket gerekmez. Yalnız yayın hazırlığı tamamlandığında yeni sürüm etiketi oluşturun. Yayımlanmış sürümlerin etiketlerini ve paketlerini değiştirmeyin.

Nightly veya development build kanalı zorunlu değildir. Şu anda yalnız kararlı release indirme kanalı var. İleride development paketleri sunulursa tarih/commit ve deneme sürümü olduğu açıkça belirtilir; stable latest bağlantısı bunlarla karıştırılmaz.

GitHub Issues bug/enhancement etiketleriyle yeterlidir. İstenirse somut bir yayın için Milestone oluşturulabilir; takvim ve bütün gelecek özellikleri peşinen taahhüt etmek gerekmez.
