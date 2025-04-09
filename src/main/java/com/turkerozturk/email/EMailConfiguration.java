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
package com.turkerozturk.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EMailConfiguration {
    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.port}")
    private int port;
    @Value("${spring.mail.username}")
    private String username;
    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.properties.mail.transport.protocol}")
    private String transportProtocol;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean smtpAuth;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private boolean smtpStarttlsEnable;
    @Value("${spring.mail.properties.mail.smtp.starttls.required}")
    private boolean smtpStarttlsRequired;
    @Value("${spring.mail.debug}")
    private boolean debug;
    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
    private static final String MAIL_SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";
    private static final String MAIL_DEBUG = "mail.debug";
    @Bean
    public JavaMailSender getJavaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);

        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();

        props.put(MAIL_TRANSPORT_PROTOCOL, transportProtocol);

        props.put(MAIL_SMTP_AUTH, String.valueOf(smtpAuth));
        props.put(MAIL_SMTP_STARTTLS_ENABLE, String.valueOf(smtpStarttlsEnable));
        props.put(MAIL_SMTP_STARTTLS_REQUIRED, String.valueOf(smtpStarttlsRequired));

        props.put(MAIL_DEBUG, String.valueOf(debug));

        return mailSender;
    }

}
