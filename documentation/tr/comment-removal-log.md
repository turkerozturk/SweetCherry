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
