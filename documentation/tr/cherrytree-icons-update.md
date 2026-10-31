# CherryTree ikon güncellemesi

İncelenen aralık: `51f6e649` **dahil**, kaynak snapshot: [`61fca35`](https://github.com/giuspen/cherrytree/commit/61fca359a4316f07965d2a35db2f7c96481c209c). Son ikon/ct_const.h değişikliği: `eeda668c`.

Kaynak: [`_NODE_CUSTOM_ICONS` ve `NODE_CODE_ICONS`](https://github.com/giuspen/cherrytree/blob/61fca359a4316f07965d2a35db2f7c96481c209c/src/ct/ct_const.h). Sonraki master değişiklikleri bu sabit bağlantıları değiştirmez.

## Manuel dosya ekleme

Bu yama SVG veya PNG dosyalarını **içermez**. Aşağıdaki SVG’leri bağlantılardan indirin ve dosya isimlerini aynen koruyun:

- Orijinal SVG: `src/main/resources/static/img/icons/<isim>.svg`
- Dönüştürülmüş PNG: `src/main/resources/static/img/ctbicons/<isim>.png`

Özellikle `ct_arrow-left` ve `ct_spring-boot` isimlerindeki **tireleri alt çizgiye çevirmeyin**. Mevcut PNG seti 16×16 pikseldir; aynı boyutta, şeffaf arka planlı PNG üretin. Orijinal SVG’leri ölçek bilgisini değiştirmeden saklayın. PNG’ler ağaç/seçici/Freeplane tarafında kullanılır. Dosyalar eklenene kadar yeni ikonların görselleri eksik kalabilir. Mevcut CherryTree lisans/atıf düzenini koruyun.

## Kimlikler ve indirme bağlantıları

`is_ro = (ikon ID << 1) | salt_okunur_biti`. Eski ID’ler değişmez.

| ID | is_ro (yazılabilir) | İsim | Orijinal SVG |
|---:|---:|---|---|
| 246 | 492 | Slovak | [ct_sk.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_sk.svg) |
| 247 | 494 | Visible | [ct_visible.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_visible.svg) |
| 248 | 496 | Invisible | [ct_invisible.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_invisible.svg) |
| 249 | 498 | Arrow Left | [ct_arrow-left.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-left.svg) |
| 250 | 500 | Arrow Right | [ct_arrow-right.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-right.svg) |
| 251 | 502 | Arrow Down | [ct_arrow-down.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-down.svg) |
| 252 | 504 | Arrow Up | [ct_arrow-up.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-up.svg) |
| 253 | 506 | Jenkins | [ct_jenkins.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_jenkins.svg) |
| 254 | 508 | PostgreSQL | [ct_postgresql.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_postgresql.svg) |
| 255 | 510 | Spring Boot | [ct_spring-boot.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_spring-boot.svg) |
| 256 | 512 | Snowflake | [ct_snowflake.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_snowflake.svg) |
| 257 | 514 | Vim | [ct_vim.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_vim.svg) |
| 258 | 516 | Locale | [ct_locale.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_locale.svg) |
| 259 | 518 | Gemini | [ct_gemini.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_gemini.svg) |
| 260 | 520 | ChatGPT | [ct_chatgpt.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_chatgpt.svg) |
| 261 | 522 | Copilot | [ct_copilot.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_copilot.svg) |
| 262 | 524 | Claude | [ct_claude.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_claude.svg) |
| 263 | 526 | Tamil | [ct_ta.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_ta.svg) |

## Commit geçmişi

- 2024-09-10: [51f6e649](https://github.com/giuspen/cherrytree/commit/51f6e649aa063944293e33fb98b658abadf8d4a7) — added support for new language Slovak (work of Michal Fusatý): `ct_sk.svg`.
- 2024-09-14: [fcf0a419](https://github.com/giuspen/cherrytree/commit/fcf0a419acff1bdaac6dc28da4ea9fbdb8624192) — implemented collapsable headers support when TOC is generated (#2389): `ct_invisible.svg`, `ct_visible.svg`.
- 2025-03-13: [9ae428ce](https://github.com/giuspen/cherrytree/commit/9ae428ce5982dbfa193bb7d934593f31d9b4e3b3) — collapsible headers, changed expanded/collapsed icon to arrows (#2389): `ct_arrow-down.svg`, `ct_arrow-left.svg`, `ct_arrow-right.svg`, `ct_arrow-up.svg`.
- 2025-03-29: [fcf9262a](https://github.com/giuspen/cherrytree/commit/fcf9262a838cb1601726b12d09240ad04edddeec) — added new icons for jenkins, postgresql, spring-boot (#2677): `ct_jenkins.svg`, `ct_postgresql.svg`, `ct_spring-boot.svg`.
- 2025-04-25: [b667d032](https://github.com/giuspen/cherrytree/commit/b667d03281d5c230ea8998565412a3f89129282c) — added snowflake icon (#2686): `ct_snowflake.svg`.
- 2025-05-11: [5fd43f89](https://github.com/giuspen/cherrytree/commit/5fd43f89ae1dcc288129dd019dc8288c817d7049) — added vim icon; added action to toggle vim mode which will be available only in gtksourceview >= 5.4 / GTK4 (#425): `ct_vim.svg`.
- 2025-05-30: [431b3bb0](https://github.com/giuspen/cherrytree/commit/431b3bb0da1fd4aa782726860d2cf74fab4d020c) — added tree icon for locale/language: `ct_locale.svg`.
- 2025-10-09: [204c2b32](https://github.com/giuspen/cherrytree/commit/204c2b322375007bd22be761c76508ef41ecb6b1) — added icons for artificial intelligence chatgpt, gemini: `ct_chatgpt.svg`, `ct_gemini.svg`.
- 2025-10-11: [0b211e36](https://github.com/giuspen/cherrytree/commit/0b211e36692b29b16aab4381a9d975a83f94e194) — added icons for artificial intelligence copilot, claude: `ct_claude.svg`, `ct_copilot.svg`.
- 2026-10-10: [eeda668c](https://github.com/giuspen/cherrytree/commit/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f) — support for new language Tamil: `ct_ta.svg`.

## CodeIcon ve seçim listesi hakkında

Kaynak `NODE_CODE_ICONS` tablosundaki 35 syntax eşleştirmesi mevcut `CodeIcon.java` ile birebir aynı. Bu nedenle CodeIcon değişmedi; örneğin `sql` hâlâ `ct_db` kullanır, `vim` için kaynakta olmayan bir syntax eşleştirmesi eklenmez. Yeni Vim/PostgreSQL/Spring Boot ikonları özel düğüm ikonu olarak seçilebilir. Tamil `ct_ta` ID 263, kaynak `_NODE_CUSTOM_ICONS` içinde tanımlıdır fakat incelenen sürümün `NODE_CUSTOM_ICONS_ORDERED` listesinde yoktur; SweetCherry’nin alfabetik enum seçicisi yine gösterebilir.

## Bookmarks ve yerel yapılandırma

Masaüstü bookmarks tablosuna **Oluşturulma** sütunu eklendi; mobilde genişliği artırmamak için oluşturulma ve güncelleme tarihleri düğüm adının üstünde etiketli gösterilir. İkisi de node tarihidir; bookmark oluşturulma tarihi değildir. Paylaşımlı konumlarda ortak içeriğin/master’ın tarihleri gösterilir. Sırası değişen bookmark’ların içerik master’ının modified tarihi senkronizasyon kuralı gereği güncellenmeye devam eder; gösterim eklemesi hiçbir tarihi değiştirmez.

Kökte oluşan `application.yml`, `application-experiments.yml`, `application-operations.yml` gitignore kapsamındadır. `src/main/resources/` içindekiler izlenmeye devam eder. Önceden izlenen bir kök dosyası varsa gitignore tek başına onu izleme dışına çıkarmaz; bu yamaya temel olan committe bu üç kök dosya izlenmiyor.

## Kontrol

18 SVG ve PNG’yi ekleyin, ikon seçicisinde yeni adları arayın, seçimleri kaydedip CherryTree’de karşılaştırın. 246–263 ID’leri ve read-only biti korunmalıdır. Desktop/mobil bookmarks sayfasında iki tarihi ve tooltip saati kontrol edin. Bir bookmark sırasını değiştirin: oluşturulma sabit kalmalı, sırası değişen düğümlerin güncelleme tarihi ilerlemelidir.
