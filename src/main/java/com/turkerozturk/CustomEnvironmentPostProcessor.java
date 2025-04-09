/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.ConfigurableEnvironment;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private final Log logger;

    // https://www.woolha.com/tutorials/spring-boot-using-environmentpostprocessor-examples
    // https://www.baeldung.com/spring-boot-environmentpostprocessor
    // https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/howto-spring-boot-application.html
    public CustomEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(CustomEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        // bu class sayesinde application.properties dosyasindan sqlite veritabanina erisim baglantisini cekiyoruz
        String dataSourceUrl = environment.getProperty("spring.datasource.url");

        // bu sayede spring boot uygulamasi daha db ve web gibi islemleri baslatmadan kendi islemlerimizi calistiriyoruz
        if(dataSourceUrl != null) { // multitenat dbs ekleyince bu ayar artik olmadigindan hata vermemesi icin null check yapiyoruz.
            // Ama artik bu metodun tamamina gerek kalmadi gibi. Yani artik veritabanlarinin ayarlarini allTenants klasorumuzdeki dosyalardan cekiyor cunku.
            callCustomMethod(dataSourceUrl);
        }

    }

    /**
     * application.properties dosyasinda belirtilen veritbaninin kontrolu ile ilgili kodlari iceriyor su anda.
     * sqlite veritabani 0 boyuttaysa dosyayi silip programdan cikar.
     * veya dosya yoksa programdan cikar.
     * @param dataSourceUrl
     */
    private void callCustomMethod(String dataSourceUrl) {

        // bu asamada loglama tam calismayacagi icin DeferredLogFactory cozumu kullaniliyor.
        // Her ihtimale karsi hem logger hem de system out println kullanarak bilgilendirme yapiyorum.
        // cunku konsolda sadece system out println yi gorebildim. Ileride tekrar konuya donulebilir.
        // simdilik yeterli.
        StringBuilder stringBuilder = new StringBuilder();

        // Veritabani dosyasinin yolunu al
		String dbPathAsString = dataSourceUrl.substring("jdbc:sqlite:".length());

		// Veritabani dosyasini kontrol et
        final Path path = Path.of(dbPathAsString);
        File dbFile = new File(dbPathAsString);
        if(Files.exists(path)) {
            if (dbFile.length() == 0) {
                stringBuilder.append("The database file length is 0 bytes.\n");
                stringBuilder.append("This indicates that the database file was not found(not an SQLite file).\n");
                try {
                    Files.delete(path);
                    stringBuilder.append("Deleted that orphan file.\n");
                } catch (IOException e) {
                    stringBuilder.append("The orphan file cannot be deleted.\n");
                }
                stringBuilder.append("Please ensure that your database file is located at the exact path below:\n");
                stringBuilder.append(dbFile.getAbsolutePath());
                logger.error(stringBuilder.toString());
                System.out.println(stringBuilder.toString());
                System.exit(1); // Uygulamayi sonlandir
            }
        } else {
            stringBuilder.append("The database file could not be found.");
            stringBuilder.append("Please ensure that your database file is located at the exact path below:\n");
            stringBuilder.append(dbFile.getAbsolutePath());
            logger.error(stringBuilder.toString());
            System.out.println(stringBuilder.toString());
            System.exit(1); // Uygulamayi sonlandir
        }

    }

}

