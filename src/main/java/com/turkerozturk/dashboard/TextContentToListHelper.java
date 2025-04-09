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
package com.turkerozturk.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextContentToListHelper {

    private static final Logger logger = LoggerFactory.getLogger(TextContentToListHelper.class);


    private Map<String, String> keyNameAndDescription;

    private Map<String, String> invalidDataMap;

    private static final String LINE_SEPARATOR = "\\r?\\n";

    private static final String COLUMN_SEPARATOR = "\\t";

    private List<String> lineHealthList;

    public TextContentToListHelper(String textContent) {


        // bilgi if textContent is not null or empty

        keyNameAndDescription = new HashMap<>();
        invalidDataMap = new HashMap<>();
        lineHealthList = new ArrayList<>();
        String[] tmp = textContent.split(LINE_SEPARATOR);

        StringBuilder sb = new StringBuilder();

        String templateName;
        String templateDescription;

        for (String t : tmp) {

            if(t.equals("")) {
                sb.append("boş satır: ");
                invalidDataMap.put(t, "BOŞ SATIRLAR VAR, TEMİZLEMENİZ İYİ OLUR.");
            } else if (t.startsWith("#")) {
                sb.append("devredışı satır: ");
                invalidDataMap.put(t, "satır # ile başladığı için dikkate alınmaz.");
            } else {
                sb.append("dolu satır: ");

                String arr[] = t.split(COLUMN_SEPARATOR);
                templateName = arr[0]; // bilgi BOS satir bile olsa MAPe ekliyoruz. Kontrolunu baska metoda birakmak icin.
                if (arr.length > 1) {
                    templateDescription = arr[1]; // bilgi description var
                    keyNameAndDescription.put(templateName, templateDescription);
                } else {
                    // bilgi sadece keyName var, o sebeple descriptionu da kendine esit yapiyoruz.
                    keyNameAndDescription.put(templateName, templateName);
                }
                logger.info("ANA DUGUMDEKI SABLON ISMI: " + templateName);

            }



        }


    }


    public Map<String, String> getKeyNameAndDescription() {
        return keyNameAndDescription;
    }

    public void setKeyNameAndDescription(Map<String, String> keyNameAndDescription) {
        this.keyNameAndDescription = keyNameAndDescription;
    }

    public Map<String, String> getInvalidDataMap() {
        return invalidDataMap;
    }

    public void setInvalidDataMap(Map<String, String> invalidDataMap) {
        this.invalidDataMap = invalidDataMap;
    }
}
