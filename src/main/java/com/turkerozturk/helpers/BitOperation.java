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

import com.turkerozturk.IconIdAndIsReadOnly;

public class BitOperation {

    /**
     * veritabaninda node.is_ro alaninda iki degisken degeri vardir.
     * Bu metod gelen bitisik degeri alip onlari parse eder.
     * @param sixteenBitAsInt
     * @return
     */
    public static IconIdAndIsReadOnly processSixteenBitData(int sixteenBitAsInt) {

        boolean isReadOnly = (sixteenBitAsInt & 1) == 1;

        // En dusuk biti temizleyerek diger 15 biti elde etme ve bir saga kaydirma
        int remainingFifteenBitsAsInt = (sixteenBitAsInt & 0x7FFF) >> 1;

        return new IconIdAndIsReadOnly(remainingFifteenBitsAsInt, isReadOnly);
    }

    /**
     * veritabaninda node.is_ro alaninda iki degisken degeri vardir.
     * Bu metod iki degiskeni alip veritabanina bitisik olarak yazilacak hale getirir.
     * @param iconIdAndIsReadOnly
     * @return
     */
    public static long concatIconIdAndIsReadOnly(IconIdAndIsReadOnly iconIdAndIsReadOnly) {

        int iconIdInPlaceAsInt = (iconIdAndIsReadOnly.iconId() << 1);

        int isReadOnlyBitInPlaceAsInt =  iconIdAndIsReadOnly.isReadOnly() ? 1 : 0;

        return iconIdInPlaceAsInt + isReadOnlyBitInPlaceAsInt;
    }

    /**
     * Encodes a node title's RGB color, bold flag, and rich text flag for node.is_richtxt.
     * Bit 2 marks a selected color; zero color clears both the color and that marker.
     * Bit 0 holds the rich text flag, while bit 1 holds the bold flag.
     */
    public static long concatNodeTitleColorAndBoldnessAndTextType(long color,
                                                                   boolean bold,
                                                                   boolean richText) {
        if (color < 0 || color > 0xFFFFFFL) {
            throw new IllegalArgumentException("Title color must be a 24-bit RGB value");
        }
        long colorBits = color << 3;
        long colorPresentBit = color == 0 ? 0 : 1L << 2;
        long boldBit = bold ? 1L << 1 : 0;
        long richTextBit = richText ? 1L : 0;
        return colorBits | colorPresentBit | boldBit | richTextBit;
    }

}
