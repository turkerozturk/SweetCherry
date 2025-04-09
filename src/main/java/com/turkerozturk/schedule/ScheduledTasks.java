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
package com.turkerozturk.schedule;

import com.turkerozturk.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ScheduledTasks {

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 * * * * *") // each minute
    public void runAtTwelve() {
        System.out.println("Scheduled task ran at: " + LocalDateTime.now());
    }


    @Scheduled(fixedRate = 86400000)
    public void sendEmail() {
        System.out.println("Scheduled email task ran at: " + LocalDateTime.now());
        // improve the code and then uncomment later:
        // emailService.sendEmail("email_message_to@example.com", "example reminder email message", "bu ornek bir hatirlatma mesajidir");
    }


}
