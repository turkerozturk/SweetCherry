# Masaüstü ağaç genişliği / Desktop tree width

Yalnız **`/tree`** sayfasındaki ağaç–içerik ayırıcısı için geçerlidir. Mobil okuyucu ve
zengin metin editörünün ayırıcısı etkilenmez.

`application.yml`:

```yaml
myapp:
  tree:
    default-width: 600
```

Değer piksel cinsindendir. Eski varsayılan 300 px idi; yeni varsayılan 600 px’tir.
Ayar değişikliğinden sonra uygulamayı yeniden başlatın. 20–1900 px aralığı uygulanır;
mevcut pencere daha darsa ağaç, ayırıcıya ve en az 20 px içerik alanına yer bırakacak
biçimde daraltılır. Eski 700 px üst sınırı ve 160 px alt sınırı kaldırılmıştır.

Ayırıcıyı fareyle sürükleme veya odaktayken sağ/sol ok tuşlarıyla değiştirme sonrasında
**yalnız genişlik** tarayıcının yerel depolamasına kaydedilir. Aynı tarayıcı/profil ve
aynı site/adres/portta yeni sekme de bu genişliği kullanır; açık sekmeler de güncellenir.
Ağaç seçimi, açık dallar ve kaydırma konumları sekmeye özel kalır. Küçük bir pencereye
otomatik sığdırma, büyük pencere için kaydedilmiş tercihi değiştirmez.

Kaydedilmiş tarayıcı tercihi YAML’deki başlangıç değerinden önceliklidir. Ayarın yeni
varsayılanını tekrar kullanmak için site depolamasını temizleyin veya geliştirici
konsolunda yalnız bu anahtarı kaldırıp sayfayı yenileyin:

```javascript
localStorage.removeItem('sweetcherry-tree-width');
```

Depolama engelliyse sayfa yine çalışır; sekmeler arası hatırlama kullanılamaz.

## English

`myapp.tree.default-width` sets the initial desktop `/tree` pane width in pixels
(default 600, previously 300). It does not affect the mobile reader or other splitters.
The range is 20–1900 pixels, further bounded by the available viewport. Restart after
changing YAML. Manual resizing stores only this width in browser local storage for
other tabs at the same origin; navigation remains per-tab. Saved preferences override
the YAML default. Narrow-window fitting does not overwrite the saved preferred width.
