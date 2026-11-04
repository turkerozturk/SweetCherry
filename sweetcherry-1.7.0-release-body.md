Draft prepared: **4 November 2026** · CherryTree CTB web companion. New: text-tree ZIP export, batch tag management, improved template reports and custom HTML/PDF record layouts.

The Java-free ZIP contains the same application and scripts for Windows, Linux/Raspberry Pi ARM and macOS; install Java 17+ for your OS/CPU. **macOS and 32-bit/i386 systems are untested. The setup EXE and bundled Java run only on Windows 10/11 x64**, despite the `all_os` filename.

| Asset | Purpose |
| --- | --- |
| `sweetcherry_1.7.0_all_os_portable.zip` | Portable app, no Java; use the OS run script |
| `sweetcherry_1.7.0_all_os_portable_with_java_and_setup.exe` | Windows x64 setup + Java 17 |
| `sweetcherry-1.7.0-third_party_licences.zip` | Licences/credits, also included in the app packages |
| `sha256sums.txt` | File checksums |

[English setup](https://github.com/turkerozturk/SweetCherry/blob/v1.7.0/documentation/en/setup-and-run.md) · [Türkçe kurulum](https://github.com/turkerozturk/SweetCherry/blob/v1.7.0/documentation/tr/installation.md) · [Changes, upgrade notes and limits](https://github.com/turkerozturk/SweetCherry/blob/v1.7.0/documentation/releases/v1.7.0.md)

Building from source requires JDK 17+: use the **entire generated `release/SweetCherry` folder**. Extract the portable ZIP completely; on Windows use `run.bat` with system Java.
