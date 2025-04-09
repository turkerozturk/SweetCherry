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

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

@SpringBootApplication
@EnableCaching
public class SweetCherry implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SweetCherry.class);

    private static ConfigurableApplicationContext context; // https://www.baeldung.com/java-restart-spring-boot-app

    public static void main(String[] args) {

        context = SpringApplication.run(SweetCherry.class, args);

    }


    @Override
    public void run(String... args) {

    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propsConfig
                = new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        propsConfig.setIgnoreResourceNotFound(true);
        propsConfig.setIgnoreUnresolvablePlaceholders(true);
        return propsConfig;
    }

    //https://spring.io/guides/gs/spring-boot
    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx,
                                               @Value("${myapp.command-line-runner.enabled:true}") boolean enabled) {
        if (enabled) {
            return args -> {
                logger.info("Let's inspect the beans provided by Spring Boot:");

                String[] beanNames = ctx.getBeanDefinitionNames();
                Arrays.sort(beanNames);
                for (String beanName : beanNames) {
                    logger.info("beanName: " + beanName);
                }
            };
        } else {
            return args -> {
            }; // Etkin degilse bos bir CommandLineRunner dondur
        }
    }

    /**
     * statik degiskenlerde @Value kullanabilmek icin setter metod olusturup onun uzerinde kullanmak gerekiyor.
     * bu cozumu chatgpt verdi.
     * "static alanlara uygulanan @Value annotation'lari Spring tarafindan yeterince erken islenemez ve bu nedenle
     * deger null olarak atanır.
     * Bunun yerine, @Value annotation'ini bir setter metodu veya constructor parametresi uzerinde kullanmaniz
     * gerekmektedir. Ancak, setter metodu veya constructor parametresi uzerinde kullanmaniz durumunda,
     * degisken degerinin null olmasini onlemek için bu alani sifirladiginizdan emin olmaniz gerekir."
     */
    public static Boolean openWebBrowserOnStartup;

    @Value("${myapp.openWebBrowserOnStartup}")
    public void setOpenWebBrowserOnStartup(Boolean openWebBrowserOnStartup) {
        this.openWebBrowserOnStartup = openWebBrowserOnStartup != null ? openWebBrowserOnStartup : false;
    }


    /**
     * This method runs immediately after the Spring Boot application has started up.
     */
    @EventListener({ApplicationReadyEvent.class})
    void applicationReadyEvent() {
        logger.info("Application started ...");
        if (openWebBrowserOnStartup) {
            logger.info("launching browser now");
            browse("http://localhost:8080");
        }
    }

    /**
     * This example is for Windows OS's only
     *
     * @param url
     */
    public static void browse(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            String[] command;

            String operatingSystemName = System.getProperty("os.name").toLowerCase();
            if (operatingSystemName.indexOf("nix") >= 0 || operatingSystemName.indexOf("nux") >= 0) {
                String[] browsers = {"opera", "google-chrome", "epiphany", "firefox", "mozilla", "konqueror", "netscape", "links", "lynx"};
                StringBuffer stringBuffer = new StringBuffer();

                for (int i = 0; i < browsers.length; i++) {
                    if (i == 0) stringBuffer.append(String.format("%s \"%s\"", browsers[i], url));
                    else stringBuffer.append(String.format(" || %s \"%s\"", browsers[i], url));
                }
                command = new String[]{"sh", "-c", stringBuffer.toString()};
            } else if (operatingSystemName.indexOf("win") >= 0) {
                command = new String[]{"rundll32 url.dll,FileProtocolHandler " + url};

            } else if (operatingSystemName.indexOf("mac") >= 0) {
                command = new String[]{"open " + url};
            } else {
                System.out.println("an unknown operating system!!");
                return;
            }

            try {
                if (command.length > 1) runtime.exec(command); // linux
                else runtime.exec(command[0]); // windows or mac
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // To run the Spring Boot HTTPS Application also through a HTTP Port use the two methods below:

    @Value("${server.http.port}")
    private int httpPort;

    private Connector connector() {

        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setScheme("http");
        return connector;

    }


    @Bean
    public ServletWebServerFactory servletWebServerFactory() {

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addAdditionalTomcatConnectors(connector());
        return factory;

    }


}
