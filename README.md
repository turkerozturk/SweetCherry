# SweetCherry

**Your CherryTree notes, through a web browser.**

SweetCherry is a free, open-source web companion for [CherryTree](https://www.giuspen.net/cherrytree/) CTB note databases. Browse your note tree on desktop or mobile, edit supported content, explore mind maps, and export notes in useful formats. It complements CherryTree and works with the same database structure.

[![Latest release](https://img.shields.io/github/v/release/turkerozturk/SweetCherry)](https://github.com/turkerozturk/SweetCherry/releases/latest)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](documentation/en/setup-and-run.md)
[![License: GPL v3 or later](https://img.shields.io/badge/License-GPL--3.0--or--later-blue)](LICENSE)

**[Download](https://github.com/turkerozturk/SweetCherry/releases/latest)** · **[English setup](documentation/en/setup-and-run.md)** · **[Türkçe kurulum](documentation/tr/installation.md)** · **[DeepWiki](https://deepwiki.com/turkerozturk/SweetCherry)** · **[Changelog](CHANGELOG.md)**

![SweetCherry desktop launchers and application folder overview](https://github.com/turkerozturk/SweetCherry/releases/download/v1.7.0/sweetcherry-application-folder.png)

## Get started

Choose a package from the [latest stable release](https://github.com/turkerozturk/SweetCherry/releases/latest):

| Package | How to start |
| --- | --- |
| **Windows x64 setup, with Java** | Run the setup EXE, choose a writable folder, and start the desktop launcher. No separate Java installation is needed. |
| **Portable ZIP, without Java** | Install Java 17+ matching your OS/CPU, extract the complete ZIP, and use the appropriate launch script. |
| **Source code** | Build with JDK 17+ and use the **entire generated `release/SweetCherry` folder**. See the [source build guide](documentation/tr/setup-and-run.md) (Turkish). |

On Linux desktop, use `launcher.sh` and optionally create shortcuts with `install-desktop.sh`. Terminal users can use `run.bat` on Windows, `run.sh` on Linux/Raspberry Pi, or `run.command` on macOS. See the [English setup guide](documentation/en/setup-and-run.md) for exact commands and requirements. macOS and 32-bit/i386 systems are untested; the bundled Windows Java runtime is Windows-only.

Open the browser from the launcher, sign in using the generated credentials, then select the included demo or quick-start data source. The [English practice workbook](https://github.com/turkerozturk/SweetCherry/releases/download/v1.7.0/sweetcherry-quickstart-en.ctb) provides hundreds of tasks and small examples. For the separately updated workbook, stop the application, preserve any existing exercises, and replace `CTBDATA/sweetcherry-quickstart-en.ctb` before starting again.

**Keep your notes safe:** start with a copy or a read-only connection. Manual CTB backups and optional scheduled backups are available; scheduling is off until configured. Keep important backups on another device, and do not write to the same CTB simultaneously from SweetCherry and CherryTree.

## What you can do

| Area | Features |
| --- | --- |
| **Read and edit** | Desktop/mobile readers; supported rich text, Plain Text, Markdown and [HTML](documentation/en/html-notes.md) editing; live preview; images, attachments, tables, codeboxes and offline LaTeX; browser-local recovery drafts. |
| **Find and organise** | Quick switch, quick note capture, advanced search, saved filters, backlinks, searchable tags, batch tag management, bookmarks, recent activity and weekly/monthly calendars. |
| **Explore the tree** | Markmap and Mermaid views; quick child creation in Markmap; balanced two-sided layout and a page-filling canvas; node movement, duplication and shared nodes. |
| **Export and exchange** | PDF with hierarchical navigation; CTB export/import with remapped internal IDs; Freeplane/FreeMind export with optional note text and icons; folder-tree text ZIP export; custom record reports and HTML/PDF layouts. |
| **Back up and recover** | Verified CTB snapshots, optional schedules and retention, backup downloads, reviewed deletion and recovery into a separate working copy. |
| **Connect and configure** | Data-source wizard; SQLite, MariaDB/MySQL and PostgreSQL connections; application settings editors; local use or explicitly configured network access. |

**On `main`:** [Freeplane/FreeMind MM import](documentation/en/freeplane-import.md) adds a reviewed mind-map tree as a new final root branch. This feature is newer than the 1.7.0 release; use a source build to try it until a later release includes it. Other source changes may also be unreleased.

## Documentation

| Start here | More detail |
| --- | --- |
| [English installation and usage](documentation/en/setup-and-run.md) | [First-start help / İlk çalıştırma](documentation/tr/first-start.md) |
| [Türkçe kurulum ve kullanım](documentation/tr/installation.md) | [English feature guides](documentation/en) · [Türkçe özellik kılavuzları](documentation/tr) |
| [Changes and upgrade notes](CHANGELOG.md) | [1.7.0 release notes](documentation/releases/v1.7.0.md) · [All release notes](documentation/releases) |

**Automatically generated documentation is also available on [DeepWiki](https://deepwiki.com/turkerozturk/SweetCherry).** Use it to explore the repository and codebase. DeepWiki is an external, automatically generated resource; check the version-matched project guides for installation instructions, supported behavior and limits.

<details>
<summary>Guides for specific tasks</summary>

- [CTB backups and recovery](documentation/en/ctb-backups.md)
- [Advanced search](documentation/en/advanced-search.md) · [Saved filters and backlinks](documentation/en/saved-filters-backlinks.md)
- [Quick switch and quick notes](documentation/tr/quick-notes.md) (Turkish)
- [Batch tag management](documentation/en/tag-management.md) · [Home activity](documentation/en/home-activity.md)
- [Markmap quick child creation](documentation/en/markmap-quick-child.md)
- [Folder-tree text ZIP export](documentation/en/text-tree-export.md)
- [Custom lists setup](documentation/en/custom-lists-setup.md) · [Template reports](documentation/en/template-reports.md) · [Record HTML/PDF layouts](documentation/en/record-layouts.md)
- [CTB export](documentation/tr/ctb-export.md) · [Shared nodes](documentation/tr/shared-node-operations.md) (Turkish)
- [Network access](documentation/tr/network-access.md) · [Optional features](documentation/tr/optional-features.md) · [Versioning](documentation/tr/versioning.md) (Turkish)

</details>

## Feedback and development

Found a problem or have an idea? [Open an issue](https://github.com/turkerozturk/SweetCherry/issues). Include your SweetCherry version, operating system and steps to reproduce; use demonstration data rather than personal notes or credentials.

SweetCherry is developed and maintained by Türker Öztürk. ChatGPT/Codex assistance was used for implementation, test code and documentation; project decisions, review and manual release validation remain with the maintainer.

Development builds are not currently published as a separate download channel. Use the stable release for ordinary use; source builds from `main` may include unreleased changes.

## License

SweetCherry is licensed under GNU GPL version 3 or later. See [LICENSE](LICENSE). Bundled third-party components retain their own licenses.
