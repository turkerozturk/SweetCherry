# CherryTree icon update

Compared from `51f6e649` inclusive through [61fca359](https://github.com/giuspen/cherrytree/commit/61fca359a4316f07965d2a35db2f7c96481c209c). Last icon/ct_const.h change: `eeda668c`.
Source: [ct_const.h](https://github.com/giuspen/cherrytree/blob/61fca359a4316f07965d2a35db2f7c96481c209c/src/ct/ct_const.h).

This patch contains enum identities, not SVG/PNG assets. Download the SVGs below manually, retain their exact filenames (including hyphens), store originals in `src/main/resources/static/img/icons/`, and make transparent 16×16 PNG copies in `src/main/resources/static/img/ctbicons/`. Keep existing CherryTree license/attribution notices. Until those files are added, new icons may appear without images.

ID remains stable; `is_ro = (ID << 1) | read_only_bit`. Existing IDs are unchanged.

| ID | is_ro without read-only | SVG |
|---:|---:|---|
| 246 | 492 | [ct_sk.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_sk.svg) |
| 247 | 494 | [ct_visible.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_visible.svg) |
| 248 | 496 | [ct_invisible.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_invisible.svg) |
| 249 | 498 | [ct_arrow-left.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-left.svg) |
| 250 | 500 | [ct_arrow-right.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-right.svg) |
| 251 | 502 | [ct_arrow-down.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-down.svg) |
| 252 | 504 | [ct_arrow-up.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_arrow-up.svg) |
| 253 | 506 | [ct_jenkins.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_jenkins.svg) |
| 254 | 508 | [ct_postgresql.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_postgresql.svg) |
| 255 | 510 | [ct_spring-boot.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_spring-boot.svg) |
| 256 | 512 | [ct_snowflake.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_snowflake.svg) |
| 257 | 514 | [ct_vim.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_vim.svg) |
| 258 | 516 | [ct_locale.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_locale.svg) |
| 259 | 518 | [ct_gemini.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_gemini.svg) |
| 260 | 520 | [ct_chatgpt.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_chatgpt.svg) |
| 261 | 522 | [ct_copilot.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_copilot.svg) |
| 262 | 524 | [ct_claude.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_claude.svg) |
| 263 | 526 | [ct_ta.svg](https://raw.githubusercontent.com/giuspen/cherrytree/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f/icons/ct_ta.svg) |

## Commits

- 2024-09-10: [51f6e649](https://github.com/giuspen/cherrytree/commit/51f6e649aa063944293e33fb98b658abadf8d4a7) — added support for new language Slovak (work of Michal Fusatý); `ct_sk.svg`.
- 2024-09-14: [fcf0a419](https://github.com/giuspen/cherrytree/commit/fcf0a419acff1bdaac6dc28da4ea9fbdb8624192) — implemented collapsable headers support when TOC is generated (#2389); `ct_invisible.svg`, `ct_visible.svg`.
- 2025-03-13: [9ae428ce](https://github.com/giuspen/cherrytree/commit/9ae428ce5982dbfa193bb7d934593f31d9b4e3b3) — collapsible headers, changed expanded/collapsed icon to arrows (#2389); `ct_arrow-down.svg`, `ct_arrow-left.svg`, `ct_arrow-right.svg`, `ct_arrow-up.svg`.
- 2025-03-29: [fcf9262a](https://github.com/giuspen/cherrytree/commit/fcf9262a838cb1601726b12d09240ad04edddeec) — added new icons for jenkins, postgresql, spring-boot (#2677); `ct_jenkins.svg`, `ct_postgresql.svg`, `ct_spring-boot.svg`.
- 2025-04-25: [b667d032](https://github.com/giuspen/cherrytree/commit/b667d03281d5c230ea8998565412a3f89129282c) — added snowflake icon (#2686); `ct_snowflake.svg`.
- 2025-05-11: [5fd43f89](https://github.com/giuspen/cherrytree/commit/5fd43f89ae1dcc288129dd019dc8288c817d7049) — added vim icon; added action to toggle vim mode which will be available only in gtksourceview >= 5.4 / GTK4 (#425); `ct_vim.svg`.
- 2025-05-30: [431b3bb0](https://github.com/giuspen/cherrytree/commit/431b3bb0da1fd4aa782726860d2cf74fab4d020c) — added tree icon for locale/language; `ct_locale.svg`.
- 2025-10-09: [204c2b32](https://github.com/giuspen/cherrytree/commit/204c2b322375007bd22be761c76508ef41ecb6b1) — added icons for artificial intelligence chatgpt, gemini; `ct_chatgpt.svg`, `ct_gemini.svg`.
- 2025-10-11: [0b211e36](https://github.com/giuspen/cherrytree/commit/0b211e36692b29b16aab4381a9d975a83f94e194) — added icons for artificial intelligence copilot, claude; `ct_claude.svg`, `ct_copilot.svg`.
- 2026-10-10: [eeda668c](https://github.com/giuspen/cherrytree/commit/eeda668c02257a4a7ecd46fa8dcab5c7aa1cad3f) — support for new language Tamil; `ct_ta.svg`.

All 35 `NODE_CODE_ICONS` mappings already match CodeIcon.java, so no speculative language mappings are added. `sql` still uses `ct_db`; the Vim icon is a selectable custom icon, not a newly invented syntax mapping. Tamil ID 263 is present in `_NODE_CUSTOM_ICONS` but absent from upstream’s `NODE_CUSTOM_ICONS_ORDERED` at this snapshot; SweetCherry’s alphabetical picker can still expose it.

Bookmarks now show Created in a desktop column, with both dates shown inline on mobile. These are node dates, not bookmark dates; shared occurrences use their content master’s timestamps. Reordering still updates modified timestamps for occurrences whose bookmark sequence changes. Viewing dates does not write anything.

The three root runtime YAML copies are ignored; source configurations under `src/main/resources/` remain tracked. No tracked root copies exist in the patch baseline.

Manual checks: add SVG/PNG assets, select and save new icons, compare IDs/read-only flags in CherryTree, and verify Created/Modified display in desktop/mobile bookmarks. Reordering must preserve creation dates. Automated tests compare every source icon ID/name and all code syntax mappings to pinned TSV snapshots.
