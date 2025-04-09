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
package com.turkerozturk.yenixmlparser;


import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.image.Image;

import java.util.*;

public class ExampleMap<T> {
    private Map<Long, T> map;

    public ExampleMap() {
        // Long anahtarlarla birlikte generic bir Map oluşturun
        map = new HashMap<>();
    }

    // Anahtar ve değer eklemek için bir metod
    public void put(Long key, T value) {
        map.put(key, value);
    }

    // Anahtara göre değer almak için bir metod
    public T get(Long key) {
        return map.get(key);
    }

    // Tüm anahtar-değer çiftlerini yazdırmak için bir metod
    public void printAllEntries() {
        for (Map.Entry<Long, T> entry : map.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }


    public Map<Long, T> sortByKey() {
        // Map girdilerini bir List'e dönüştürün
        List<Map.Entry<Long, T>> list = new LinkedList<>(map.entrySet());

        // Comparator kullanarak girdileri Long değerine göre sıralayın
        Collections.sort(list, new Comparator<Map.Entry<Long, T>>() {
            public int compare(Map.Entry<Long, T> o1, Map.Entry<Long, T> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        // Sıralanmış girdilerden yeni bir Map oluşturun (LinkedHashMap kullanarak sıralı kalmasını sağlayın)
        Map<Long, T> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Long, T> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }



    public static void main(String[] args) {
        // Örnek bir Map oluşturun
        ExampleMap<Object> exampleMap = new ExampleMap<>();

        // Farklı türlerde değerler ekleyin
        exampleMap.put(1L, new Image());
        exampleMap.put(2L, new CodeBox());
        exampleMap.put(3L, new Grid());
    //    exampleMap.put(4L, new Element());
        exampleMap.put(5L, new Anchor());

        // Tüm girdileri yazdırın
        exampleMap.printAllEntries();
    }





}




