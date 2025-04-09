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
package com.turkerozturk.language;

// Asagidaki iki link karisik gittim. Ikisinden de birseyler kullandim.
// https://www.baeldung.com/spring-boot-internationalization
// https://github.com/eugenp/tutorials/blob/master/jhipster-6/bookstore-monolith/src/main/java/com/baeldung/jhipster6/config/LocaleConfiguration.java

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

@Configuration
public class LocaleConfiguration implements WebMvcConfigurer {

    private static final String LANGUAGE = "lang";

    /*
    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(new Locale("tr", "TR"));
        // slr.setDefaultLocale(Locale.US);
        return slr;
    }
    */

    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver();
        clr.setDefaultLocale(new Locale("tr", "TR"));
        clr.setCookieName("locale");
        clr.setCookieMaxAge(3600); // Set the cookie max age as per your requirement
        return clr;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName(LANGUAGE);
        registry.addInterceptor(localeChangeInterceptor);
    }
}
