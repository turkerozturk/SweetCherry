# Geçici yorum arşivi

Release öncesinde bu dosyayı yerel olarak yedekleyip depodan kaldırabilirsiniz. Bu kayıt kaldırılan özgün metinleri korur; çalışır yapılandırma değildir. Yeni temizlemelerde sona kayıt ekleyin.

## `pom.xml`

```text
<!-- yapay zeka, asagiaki bloga gerek olmadigini soyledigi icin comment ettim. -->
				<!-- Zaten bu islem yapiliyormus, tekrar yapmaya gerek yokmus. -->
				<!--
				<executions>
					<execution>
						<id>copy-resources</id>
						<phase>validate</phase>
						<goals>
							<goal>copy-resources</goal>
						</goals>
						<configuration>
							<outputDirectory>${project.build.directory}/classes</outputDirectory>
							<resources>
								<resource>
									<directory>src/main/resources</directory>
								</resource>
							</resources>
						</configuration>
					</execution>
				</executions>
				-->
```

## `pom.xml`

```text
<!-- DIKKATE AL: CherryTree CTB icin zorunlu degildir; uzak veritabanlarina export ozelligi icin tutulur. -->
```

## `pom.xml`

```text
<!-- DIKKATE AL: CherryTree CTB icin zorunlu degildir; uzak veritabanlarina export ozelligi icin tutulur. -->
```

## `src/main/resources/application.yml`

```text
# Asagidaki 4 satirin aciklamasi kisisel notlar node/6443 icinde. Bu dosyada degil de JAR olustuktan sonra ayni klasorde
# bir application.properties dosyasi olusturup onun icinde dort satir kullanildiginda lets encrypt sertifikasiyla
# calistigi gorulur. Bu satirlar olmaz ise o zaman ayni portlardan sertifikasiz yani http olarak calisir, simdiki gibi.
```

## `src/main/resources/application.yml`

```text
# DIKKATE AL: Asagidaki eski "endpoints.shutdown" yazimi Spring Boot 1.x donemindendi.
# Guncel ayar management.endpoint.shutdown.enabled degiskenidir ve operations profilinde bulunur.
# endpoints:
#   shutdown:
#     enabled: true
```

# Login ve About sadeleştirmesi

Bu bölüm yorumlarla birlikte kaldırılan arayüz metinlerini de korur.

## `src/main/resources/templates/login/login.html`

```text
    <img class="mb-4" th:src="@{/img/logo.jpg}" alt="" width="72" height="72">
```

## `src/main/resources/templates/login/login.html`

```text
<h1 class="h3 mb-3 font-weight-normal" th:text="#{login.sign_in_to_your_account}">Please sign in</h1>
```

## `src/main/resources/templates/login/login.html`

```text
<p class="mt-5 mb-3 text-muted"
       th:text="#{login.a_webviewer_for_cherrytree}">A WebViewer For CherryTree - 2024</p>
```

## `src/main/resources/templates/about/about.html`

```text
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.w3.org/1999/xhtml"
      layout:decorate="~{_layout}">
<head>
    <title>About</title>
</head>
<body>
<section layout:fragment="content">



    <p th:text="#{about.developer}">

    </p>


    <p th:text="#{about.content}">

    </p>
    <p>

    </p>
    <p>

    </p>
    <p>

    </p>
    <p>

    </p>
    <p>

    </p>
    <p>

    </p>

</section>
</body>
</html>
```

## `src/main/resources/messages.properties`

```text
login.a_webviewer_for_cherrytree=A WebViewer For CherryTree - 2024
```

## `src/main/resources/messages.properties`

```text
login.sign_in_to_your_account=Please sign in
```

## `src/main/resources/messages.properties`

```text
login.signin=Sign in
```

## `src/main/resources/messages.properties`

```text
about.content=It allows remote access by displaying it from the web browser.\
It speeds up access to content.\
It can display data together only thanks to the additional features it has. Accordingly, it can present an agent view and reminders, highlighted contents, and PDF reports.\
It can play background sound for alarms and ambient music created by the user for concentration.\
It provides some offline astronomy information about the positions of the moon and sun.
```

## `src/main/resources/messages.properties`

```text
root.about.description=You are using the freeware version.\
This software uses the database of the desktop application called CherryTree to only read data.\
2024, Türker Öztürk
```

## `src/main/resources/messages_en.properties`

```text
login.a_webviewer_for_cherrytree=A WebViewer For CherryTree - 2024
```

## `src/main/resources/messages_en.properties`

```text
login.sign_in_to_your_account=Please sign in
```

## `src/main/resources/messages_en.properties`

```text
login.signin=Sign in
```

## `src/main/resources/messages_en.properties`

```text
about.content=It allows remote access by displaying it from the web browser.\
It speeds up access to content.\
It can display data together only thanks to the additional features it has. Accordingly, it can present an agent view and reminders, highlighted contents, and PDF reports.\
It can play background sound for alarms and ambient music created by the user for concentration.\
It provides some offline astronomy information about the positions of the moon and sun.
```

## `src/main/resources/messages_en.properties`

```text
root.about.description=You are using the freeware version.\
This software uses the database of the desktop application called CherryTree to only read data.\
2024, Türker Öztürk
```

## `src/main/resources/messages_tr.properties`

```text
login.a_webviewer_for_cherrytree=CherryTree İçin Bir Web Görüntüleyici - 2024
```

## `src/main/resources/messages_tr.properties`

```text
login.sign_in_to_your_account=Lütfen giriş yapın
```

## `src/main/resources/messages_tr.properties`

```text
login.signin=Giriş
```

## `src/main/resources/messages_tr.properties`

```text
about.content=Web gezgininden göstermesi sayesinde uzaktan erişim imkanı verir.\
İçeriğe erişimi hızlandırır.\
Verileri sadece kendisinde bulunan ek özellikler sayesinde birarada görüntüleyebilir. Buna bağlı olarak ajana görünümü ve hatırlatıcılar, vurgulanmış içerikler, PDF raporlar sunabilir.\
Arkaplanda alarm için ses ve konsantrasyon için kullanıcının oluşturacağı ortam müziklerini çalabilir.\
Ay ve güneşin durumları ile ilgili birtakım çevrimdışı astronomi bilgileri sunar.
```

## `src/main/resources/messages_tr.properties`

```text
root.about.description=Freeware sürümü kullanmaktasınız.\
Bu yazılım, CherryTree isimli masaüstü uygulamasına ait veritabanını, sadece veri okuyacak biçimde kullanır.\
2024, Türker Öztürk
```


## 2026-10-07 — Düğüm PDF servisi

Kaynak: `src/main/java/com/turkerozturk/pdf/PdfFromHtmlController.java`

Eski node endpoint gövdesi ayrı PDF servisine taşınırken kaldırılan yorumlar:

```java
// DATA (we are not using it for now, will use later)
// TEMPLATE ENGINE
// STRING HTML TO BYTE ARRAY PDF CONVERSION
// PREPARATION FOR PDF FILE DOWNLOAD
// SERVING PDF FILE
```

## Şablon PDF yenilemesi

Kaynak: `src/main/java/com/turkerozturk/pdf/PdfFromHtmlController.java`

```java
// DATA (we are not using it for now, will use later)
// TEMPLATE ENGINE
// STRING HTML TO BYTE ARRAY PDF CONVERSION
// PREPARATION FOR PDF FILE DOWNLOAD
// SERVING PDF FILE
```

## Paylaşımlı düğüm gösterimi düzeltmesi — 2026-10-07

Dosya: `src/main/java/com/turkerozturk/pdf/NodePdfExportService.java`

Kaldırılan, yanlış varsayımı anlatan yorum:

```java
// Shared occurrences are leaves in the readers; do not expand the master's subtree.
```

Master’ın alt ağacını ödünç almama kuralı korunmuştur; paylaşımlı konumun kendi çocukları artık gösterilir.


## Paylaşımlı ebeveynlerde taşıma seçenekleri — 2026-10-07

Dosya: `src/main/java/com/turkerozturk/node/moving/NodeMoveService.java`

```java
/** Rejects broken parent chains and aliases with children before proposing a move. */
// A real parent must exist in node, not just in the hierarchy table.
```

Dosya: `src/main/java/com/turkerozturk/node/duplication/NodeDuplicationService.java`

```java
/** Rejects a cyclic or missing parent chain and unsupported placement underneath a shared node. */
```

Bu yorumlar paylaşımlı ebeveyni geçersiz sayan eski varsayımı içeriyordu. Döngü, eksik ebeveyn ve eksik master içeriği korumaları devam eder.


## Paylaşımlı alt ağaç işlemleri — 2026-10-08

Dosya: `src/main/java/com/turkerozturk/node/NodeDeletionService.java`

```java
// A shared node is only a reference. Deleting it must never delete its master.
// References to deleted real nodes may live outside the selected subtree.
// Validate first: a shared node with children needs an explicit migration policy.
// findAllSubChildren lists parents before descendants; delete in reverse order.
// Native bulk deletes avoid keeping a managed Node with a now-deleted Bookmark reference.
// The table name is supplied only by the fixed calls above; the node id is bound.
```

Dosya: `src/test/java/com/turkerozturk/node/duplication/NodeDuplicationServiceTest.java`

```java
// Subtree copying through a shared parent remains deferred; ordinary controls still load.
```

## `src/main/java/com/turkerozturk/node/creation/ChildNodeService.java` (2026-10-08)

Paylaşımlı parent desteği açılırken kaldırılan, artık geçerli olmayan yorum:

```java
// Shared nodes cannot own children.
```

## `src/main/java/com/turkerozturk/export/ExportRecursiveController.java` (2026-10-08)

Eski export akışı ortak JDBC servisine taşınırken kaldırılan yorumlar:

```text
/**
 * node_leri korur. Bir dugumu altindaki tum dugumlerle birlikte veritabaniAdi + dugumId seklinde ayri bir dosyaya kaydeder.
 * Oyle bir dosya onceden varsa icini silmesi gerekir.
 */
/**
     * Children tablosunda olmayan bir node_id belirlemek gerekiyor,
     * neden Node tablosu degil cunku onda shared node id ler yok.
     * @param conn
     * @return
     * @throws SQLException
     */
// Benzersiz node_id belirleme
// try (Statement stmt = conn.createStatement();
//      ResultSet rs = stmt.executeQuery(getNodeIdQuery)) {
//  }
//long newNodeId = getNewNodeId(conn);
//long newSequenceId = newNodeId;
//childrenService.findById(nodeId);
// Insert Node data
//pstmt.setLong(1, node.getNodeId());
// Insert Bookmark data
//pstmt.setLong(1, bookmark.getNodeId());
// Insert Grid data
//pstmt.setLong(1, grid.getId().getNodeId());
// Insert CodeBox data
//pstmt.setLong(1, codeBox.getId().getNodeId());
// Insert Image data
//pstmt.setLong(1, image.getNodeId());
// Shared Node oldugundan id ve verisi sadece children tablosunda tek satir kayit olarak var.
// Insert Children data
//pstmt.setLong(1, children.getNodeId());
// it is main node
// security: bu metodu ekleme sebebi, URL elle yazilirsa hata mesaji goruntulenmeden node content sayfasina yonlendirmek.
//https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-sql.html
// bilgi basla
// bilgi bitti
```

## `src/main/java/com/turkerozturk/export/ExportManipulatedRecursiveController.java` (2026-10-08)

Eski export akışı ortak JDBC servisine taşınırken kaldırılan yorumlar:

```text
// Benzersiz node_id belirleme
// try (Statement stmt = conn.createStatement();
//      ResultSet rs = stmt.executeQuery(getNodeIdQuery)) {
//  }
// Benzersiz sequence belirlemek, eger father_id 0 ise. father_id yi de biz 0 yapiyoruz zaten,
// her baska node tree ekledigimizde koke yerlessin diye.
// try (Statement stmt = conn.createStatement();
//      ResultSet rs = stmt.executeQuery(getNodeIdQuery)) {
//  }
// long newNodeId = getNewNodeId(nodeId);
//   long newSequenceId = newNodeId;
//childrenService.findById(nodeId);
// Insert Node data
//pstmt.setLong(1, node.getNodeId());
// Insert Bookmark data
//pstmt.setLong(1, bookmark.getNodeId());
//pstmt.setLong(2, newSequenceId);
// Insert Grid data
//pstmt.setLong(1, grid.getId().getNodeId());
// Insert CodeBox data
//pstmt.setLong(1, codeBox.getId().getNodeId());
// Insert Image data
//pstmt.setLong(1, image.getNodeId());
// Shared Node oldugundan id ve verisi sadece children tablosunda tek satir kayit olarak var.
// Insert Children data
//pstmt.setLong(1, children.getNodeId());
//pstmt.setLong(2, newRootNodeId + children.getFatherId());
// 0 or null
// it is main node
// 0 or null
//pstmt.setLong(2, 0);
//pstmt.setLong(3, children.getSequence());
//pstmt.setLong(3, newSequenceId);
//pstmt.setLong(4, newRootNodeId + children.getMasterId());
//https://docs.spring.io/spring-boot/docs/2.1.13.RELEASE/reference/html/boot-features-sql.html
// bilgi basla
// bilgi "it is necessary" to set it.
// bilgi bitti
//model.addAttribute("contentText", "export başarılı.");
```
