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
package com.turkerozturk.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateTimeHelper {

    public static long convertJavaDateToUnixEpochAsSeconds(Date date) {

        long epoch = date.getTime() / 1000;

        return epoch;
    }

    public static String convertTimestampToHumanReadable(double timestamp) {

        // Tamsayı kısmını ve ondalık kısmını ayırma
        long seconds = (long) timestamp;
        int nanoseconds = (int) ((timestamp - seconds) * 1_000_000_000L);

        // Tarih ve zaman nesnesine dönüştürme
        Instant instant = Instant.ofEpochSecond(seconds, nanoseconds);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        // Biçimlendirme
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formattedDateTime = dateTime.format(formatter);

        // Sonucu yazdırma
        //System.out.println("Tarih ve zaman: " + formattedDateTime);
        return formattedDateTime;
    }

    public static long convertToUnixTimestamp(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            // Verilen tarihi Date nesnesine çevir
            Date date = dateFormat.parse(dateString);
            // Date nesnesini Unix zaman damgasına dönüştür (milisaniye cinsinden)
            long unixTimestamp = date.getTime();
            // Milisaniyeleri saniyelere dönüştür
            return unixTimestamp / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Hata durumunda 0 değerini dön
        }
    }

    public static long convertToUnixTimestampAtDayBegin(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // Verilen tarihi Date nesnesine çevir
            Date date = dateFormat.parse(dateString);
            // Date nesnesini Unix zaman damgasına dönüştür (milisaniye cinsinden)
            long unixTimestamp = date.getTime();
            // Milisaniyeleri saniyelere dönüştür
            return unixTimestamp / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Hata durumunda 0 değerini dön
        }
    }

    public static long convertToUnixTimestampAtDayEnd(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // Verilen tarihi Date nesnesine çevir
            Date date = dateFormat.parse(dateString);
            // Date nesnesini Unix zaman damgasına dönüştür (milisaniye cinsinden)
            long unixTimestamp = date.getTime();
            // Milisaniyeleri saniyelere dönüştür
            return unixTimestamp / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Hata durumunda 0 değerini dön
        }
    }

    public static String getCurrentDateInFormat(String format) {
        // Bugünün tarihini al
        LocalDate currentDate = LocalDate.now();
        // Belirtilen formata göre tarihi biçimlendir
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        // Biçimlendirilmiş tarihi bir dize olarak döndür
        return currentDate.format(formatter);
    }

    public static String formatDate(LocalDate date, String format) {
        // Belirtilen formata göre tarihi biçimlendir
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        // Biçimlendirilmiş tarihi bir dize olarak döndür
        return date.format(formatter);
    }

    public static long convertFormDateWithTimeToUnixEpochSeconds(Date givenDate, TimeEnum timeEnum) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(givenDate);

        // Saat, dakika ve saniye kısmını sıfırla
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (timeEnum == TimeEnum.END_OF_DAY) {
            // Günün sonuna ayarla (23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        }

        long unixEpochSeconds = calendar.getTimeInMillis() / 1000;

        return unixEpochSeconds;
    }
}
