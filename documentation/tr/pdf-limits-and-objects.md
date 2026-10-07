# PDF sınırları ve nesne seçimleri

Düğüm ve alt ağaç PDF sihirbazlarında resim, kod kutusu, tablo ve LaTeX seçenekleri varsayılan olarak açıktır. Başarılı indirmeden sonra tercihler oturum boyunca hatırlanır. Kapalı nesnenin yerinde örneğin `<image object: 53:1968>` yazılır. Kimlik, **düğüm kimliği:metin içindeki konum** biçimindedir; image/grid/codebox kayıtlarının bağımsız tek sayısal kimliği yoktur.

Bu seçimler yalnız PDF çıktısını etkiler. CTB, okuyucu görünümleri ve düzenleyiciler değişmez. Dosya ekleri dosya adı ve gömülü ataş simgesiyle korunur; simge için emoji fontu gerekmez. Çapalar nesne filtrelerinden etkilenmez.

LaTeX, CherryTree’nin `__ct_special.tex` özel dosya adıyla ayrılır. Açıkken önceki dosya eki gösterimi korunur; kapalıyken `<latex object: ...>` yer tutucusu gösterilir. Bu çalışma LaTeX formül derleyicisi eklemez ve harici bir program çalıştırmaz. Format referansı: https://github.com/giuspen/cherrytree/issues/2846

## application.yml

JAR dışındaki mevcut `application.yml` dosyasına, varsa `myapp` bölümüne birleştirin:

```yaml
myapp:
  pdf:
    max-nodes: 512
    max-depth: 64
    max-node-text-characters: 8388608
    max-html-characters: 67108864
    max-image-bytes: 50331648
    max-image-pixels: 40000000
```

| Ayar | Kapsam |
| --- | --- |
| `max-nodes` | Alt ağaçtaki en fazla düğüm/occurrence sayısı |
| `max-depth` | Kökün altındaki en fazla seviye; kök 0 |
| `max-node-text-characters` | Tek düğümün ham metin/XML uzunluğu; Java karakter sayısı |
| `max-html-characters` | Tek düğümün veya alt ağacın toplam işlenmiş HTML uzunluğu |
| `max-image-bytes` | Her düğüm bölümündeki gömülü PNG verilerinin toplam byte sayısı |
| `max-image-pixels` | Tek resimde genişlik × yükseklik |

Değerler mevcut sınırlarla aynıdır; pozitif tam sayı olmalıdır. Ayar değişikliğinden sonra uygulamayı yeniden başlatın. Daha güçlü cihazlarda ilgili sınırları yükseltebilirsiniz; örneğin büyük ağaç için `max-nodes`, büyük resimler için byte/piksel ve HTML sınırları birlikte değerlendirilir. Yüksek sınır bellek ayırmaz; Java heap kapasitesi ve PDF üretiminin ek bellek kullanımı hâlâ belirleyicidir. Eşzamanlı tek düğüm/alt ağaç PDF işi sınırı korunur. Sınır aşımında 413 döner ve kırpılmış PDF sunulmaz.

Bu ayarlar düğüm ve alt ağaç PDF yollarında kullanılır. Şablon PDF’nin mevcut varsayılan belge sınırları bu aşamada değişmez.
