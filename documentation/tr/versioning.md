# Sürümleme ve release akışı

Uygulama sürümünün tek kaynağı `pom.xml` içindeki proje `<version>` alanıdır; Spring Boot parent sürümü değildir. `application.yml` içindeki `info.app.version: "@project.version@"` Maven resource filtering ile doldurulur. Git bilgileri ayrıca `git.properties` dosyasına yazılır. JAR dışındaki YAML'de eski bir sabit `info.app.version` varsa kaldırın.

İlk yayın **1.0.0**, Git etiketi **v1.0.0** olarak hazırlanmıştır. Her commit için sürüm artırılmaz; derlemeler commit kimliğiyle ayrılır.

`MAJOR.MINOR.PATCH` yaklaşımı kullanılır:

- `1.0.1`: uyumlu hata düzeltmeleri.
- `1.1.0`: uyumlu yeni özellikler.
- `2.0.0`: belgelenmiş kullanım, tenant ayarları veya desteklenen CTB davranışlarında uyumsuz değişiklikler.

Bu numaralandırma bütün CherryTree özelliklerinin uygulandığı veya hatasızlık garantisi verildiği anlamına gelmez. Sınırlamalar sürüm notlarında açıklanır. Kaynak: https://semver.org/

Yayın hazırlığını commit/push edin, etiketi aynı commit'e koyun ve son dağıtımı o etiketten oluşturun. Yayımlanmış etiketi taşımayın; aynı sürümün dosyalarını farklı bir derlemeyle değiştirmeyin. Düzeltme yeni bir sürüm olur. Yayın sonrasında main üzerinde sıradaki çalışma sürümü örneğin `1.0.1-SNAPSHOT` olabilir.

Adım adım işlem ve asset listesi: [yayın hazırlığı](release-checklist.md). Geçici yorum arşivi bu hazırlıkta depodan kaldırılmıştır; verilen yedek dosyasını yamayı uygulamadan önce yerelinizde saklayın. Git geçmişi de korunur.
