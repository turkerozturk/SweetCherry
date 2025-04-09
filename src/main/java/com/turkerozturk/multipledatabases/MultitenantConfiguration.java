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
package com.turkerozturk.multipledatabases;

import com.turkerozturk.helpers.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * https://www.baeldung.com/multitenancy-with-spring-data-jpa
 */
@Configuration
public class MultitenantConfiguration implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MultitenantConfiguration.class);
    //   @Value("${defaultTenant:tenant_1}") // olur da properties dosyasinda belirtilmezse,
    //   varsayilan veritabani baglantisi tenant_1.properties dosyasinda anlaminda.
    // private String defaultTenant;
    private ApplicationContext applicationContext;
    public static final String PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES = "allTenants";

    @Autowired
    private CustomPropertiesHolder customPropertiesHolder;

    @Bean
    @ConfigurationProperties(prefix = "tenants")
    public DataSource dataSource() {
        return createDataSource();
    }

    public DataSource createDataSource() {
        AbstractRoutingDataSource dataSource = new MultitenantDataSource();
        Map<Object, Object> resolvedDataSources = getResolvedDataSources();


        // bilgi: Acilista hicbir veritabani kaynagi belirtmek zorunda degiliz. Kisisel isim verme gerektiren hicbir
        //  properties degiskeni belirtmeye gerek yok. Asagida belirtilenleri uygulamak kosuluyla.
        //  Asagidaki satiri uncomment edersen, allTenants klasorundeki properties dosyalari icinde defaultTenant
        //  degiskenindeki stringi bulamazsa asagidaki hata mesajini verir. Program calismaya devam eder ama hata mesaji
        //  hem konsol hem de frontendde gorulecegi icin cirkin olur:
        //  java.lang.IllegalStateException: Cannot determine target DataSource for lookup key [null]
        //  Bunun olmamasi icin ya yukaridaki kosul gerceklesmeli veya asagidaki linkteki cozum application.yml
        //  dosyasina uygulanmalidir. application.yml cozumu uygulandi yani hicbir db olmasa bile hatasiz olarak
        //  uygulama basliyor.
        //  https://www.baeldung.com/spring-data-jpa-run-app-without-db
        //  Ozetle:
        //  (1) First, let’s disable the metadata fetch:
        //  spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false
        //  (2) Then, we disable the automatic database creation:
        //  spring.jpa.hibernate.ddl-auto=none #bu ayar zaten vardi bende, ustteki satir yoktu.
        //  Mantigi su:
        //  Unlike earlier, now, when we start the application, it starts without any errors.
        //  Unless an operation requires interaction with the database, a connection will not be initiated.
        // dataSource.setDefaultTargetDataSource(resolvedDataSources.get(defaultTenant));
        dataSource.setTargetDataSources(resolvedDataSources);

        dataSource.afterPropertiesSet();
        return dataSource;

    }

    public String getAllTenantsFolder() {
        return PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES;
    }

    Map<Object, Object> getResolvedDataSources() {

        //Path p = Path.of(TargetFilePathHelper.getTargetPath(PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES));
        Path p = Path.of(Paths.get(".").toAbsolutePath().normalize().toString() + File.separator + PATH_OF_ALL_DATA_SOURCE_CONNECTION_FILES);
        logger.info("PATH: " + p.toString());


        try {
            //TenantContext.setDefaultTenant(); // Varsayılan tenant'ı ayarla
            if (!Files.exists(p)) {
                logger.warn("Data Source Folder \"" + p + "\" is not Found!" +
                        " Trying to create...");
                Files.createDirectories(p);
                logger.info("Data Source Folder \"" + p + "\" is created!");
            } else {


            }
        } catch (IOException e) {

            logger.error("Cannot create Data Source Folder \"" + p + "\"!" +
                    " Please create it within the same folder where this application is.");

        }

        Map<Object, Object> resolvedDataSources = new HashMap<>();

        File[] files = p.toFile().listFiles(); // bu klasorde ayri dosyalar halinde tum veritabanlarina ait baglantilar

        assert files != null;
        for (File propertyFile : files) {
            Properties tenantProperties = new Properties();
            DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();

            String errorMessage =
                    String.format("Problematic %s file in %s folder. The error message is:\n",
                            propertyFile.getName(), p);

            try (FileInputStream fis = new FileInputStream(propertyFile);
                 InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                tenantProperties.load(inputStreamReader);
                String dsHumanFriendlyName = tenantProperties.getProperty("name");
                String dsDriverClassName = tenantProperties.getProperty("datasource.driver-class-name");
                String dsUrl = tenantProperties.getProperty("datasource.url");
                String dsUsername = tenantProperties.getProperty("datasource.username");
                String dsPassword = tenantProperties.getProperty("datasource.password");

                if (dsHumanFriendlyName != null && dsDriverClassName != null && dsUrl != null
                        && !dsHumanFriendlyName.isEmpty() && !dsDriverClassName.isEmpty() && !dsUrl.isEmpty()) {
                    dataSourceBuilder.driverClassName(dsDriverClassName);
                    dataSourceBuilder.username(dsUsername);
                    dataSourceBuilder.password(dsPassword);
                    dataSourceBuilder.url(dsUrl);
                    if (!resolvedDataSources.containsKey(dsHumanFriendlyName)) {
                        resolvedDataSources.put(dsHumanFriendlyName, dataSourceBuilder.build());
                    } else {
                        resolvedDataSources.put(dsHumanFriendlyName +
                                "(random: " + StringHelper.generateRandomString(3) + ")",
                                dataSourceBuilder.build());
                    }
                    logger.info("Data source: " + dsHumanFriendlyName +
                            ", db: " + tenantProperties.getProperty("datasource.url"));

                    // bilgi: CustomPropertiesHolder.java adinda bir Component olusturduk ve buraya inject ettik.
                    //  Bu sayede veritabani baglanti bilgilerini iceren dosyalara custom. ile baslayan kendi baska
                    //  amacli degiskenlerimizi yukleyip program genelinde getCustomProperties(String tenantName)
                    //  metodu ile alip kullanabilecegiz. chatgpy onerdi. Simdi her data source baglanti bilgisini elde
                    //  ederken custom degiskenlerimizi de bu yolla elde etmis oluyoruz.
                    // BASLA custom properties
                    // Store additional properties (custom)
                    Map<String, String> customProperties = new HashMap<>();
                    tenantProperties.stringPropertyNames().stream()
                            .filter(key -> key.startsWith("custom."))
                            .forEach(key -> customProperties.put(key, tenantProperties.getProperty(key)));
                    customProperties.put("propertyFileName", propertyFile.getName());
                    // Inside the loop in getResolvedDataSources
                    customPropertiesHolder.addCustomProperties(dsHumanFriendlyName, customProperties);
                    // Optionally, log or store the custom properties somewhere for later use
                    customProperties.forEach((key, value) ->
                            logger.info("Custom Property [" + key + "] = " + value + "(" + dsHumanFriendlyName + ")"));
                    // BITTI custom properties

                } else {
                    logger.warn(String.format("Skipped problematic '%s' file in %s folder.",
                            propertyFile.getName(), p));
                }
            } catch (UnsatisfiedDependencyException e) {
                logger.error(errorMessage + "UnsatisfiedDependencyException: " + e.getMessage());
            } catch (FileNotFoundException e) {
                logger.error(errorMessage + "FileNotFoundException: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                logger.error(errorMessage + "IllegalArgumentException: " + e.getMessage());
            } catch (IOException e) {
                logger.error(errorMessage + "IOException: " + e.getMessage());
                throw new RuntimeException("Problem in tenant datasource: " + e);
            }
        }


        return resolvedDataSources;
    }

    public void reloadDataSource() {
        DataSource dataSource = createDataSource();
        ((MultitenantDataSource) dataSource()).setTargetDataSources(getResolvedDataSources());
        ((MultitenantDataSource) dataSource()).afterPropertiesSet();
        logger.info("Data sources reloaded.");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
