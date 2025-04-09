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
/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/morethanpomodoro
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
package com.turkerozturk.cherryxml;

import com.turkerozturk.anchor.Anchor;
import com.turkerozturk.codebox.CodeBox;
import com.turkerozturk.grid.Grid;
import com.turkerozturk.image.Image;
import org.python.google.common.io.Files;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Test20240331Parser {

    public static void main(String[] args) {
        /*
        cherryImagesMap.put(79, new Image(nodeId, 79, "left", "LexerEnum20240316.ods"));
        cherryImagesMap.put(441, new Image(nodeId, 441, "left", ""));
        cherryImagesMap.put(571, new Image(nodeId, 571, "left", ""));
        cherryImagesMap.put(2070, new Image(nodeId, 2070, "left", ""));
        cherryImagesMap.put(2095, new Image(nodeId, 2095, "left", ""));
*/

        NodeTxtXmlTransformer parselleMain = new NodeTxtXmlTransformer();

        try {
            String newXMLContent = parselleMain.parse(xmlContent, nodeId, cherryAnchorsMap, cherryImagesMap, cherryCodeBoxesMap, cherryGridsMap);
            Files.write(newXMLContent.getBytes(), new File("testTransformedNodeTxt.xml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

    }



    // bilgi

    static long nodeId = 5258;

    static Map<Integer, Anchor> cherryAnchorsMap = new HashMap<>();


    static Map<Integer, Image> cherryImagesMap = new HashMap<>();


    static Map<Integer, CodeBox> cherryCodeBoxesMap = new HashMap<>();


    static Map<Integer, Grid> cherryGridsMap = new HashMap<>();


    // bilgi


    static String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<node><rich_text link=\"webs https://pygments.org/docs/lexers/\">https://pygments.org/docs/lexers/</rich_text><rich_text>\n" +
            "\n" +
            "sonuç enum tablosu libreoffice calc olarak:\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "Tabloyu nasıl oluşturduğumun aşamaları ve çalışır vaziyette yapay zekanın oluşturduğu enum kodu, o koda benim tabloyu ekleyince işlem tamam olmuış oldu:\n" +
            "\n" +
            "çok satırlı bir metinde class pygments.lexers.textfmts.NotmuchLexergibi başında class sonunda Lexer yazan tüm satırları bulabilen bir regez üret\n" +
            "\n" +
            "\n" +
            "\n" +
            "\n" +
            "</rich_text><rich_text background=\"#c6c646460000\">\\bclass\\s+(.*Lexer)\\b</rich_text><rich_text>\n" +
            "\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "find all deyip sonucu başka metin belgesine yapıştırdıktan sonra sol taraftan temizlemek için:\n" +
            ".*class \n" +
            "replace all yapıyoruz.\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "Aşağıda çok az bir pürüz kaldı, bazı lexerlerin sağında işaret var.\n" +
            "biraz da açıklama var, mesela PythonLexer piton version 3 içinmiş.\n" +
            "versiyon 2 için olanı Python2Lexer. Ama bu sonrada yapılmış yani ben eski kütüphaneyi kullanıyorum şu an belki onda öyle değildir.\n" +
            "\n" +
            "pygments.lexers.ada.AdaLexer\n" +
            "pygments.lexers.actionscript.ActionScript3Lexer\n" +
            "pygments.lexers.actionscript.ActionScriptLexer\n" +
            "pygments.lexers.actionscript.MxmlLexer\n" +
            "pygments.lexers.ada.AdaLexer\n" +
            "pygments.lexers.algebra.BCLexer\n" +
            "pygments.lexers.algebra.GAPConsoleLexer\n" +
            "pygments.lexers.algebra.GAPLexer\n" +
            "pygments.lexers.algebra.MathematicaLexer\n" +
            "...\n" +
            "....\n" +
            "...\n" +
            "\n" +
            "Yukarıdaki temizleyip enumu oluşturmak için gerekli listeleri alt düğümlerde oluşturdum.\n" +
            "\n" +
            "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "enumu ise yapay zekaya oluşturttum:\n" +
            "\n" +
            "şimdi LexerEnum adında bir java enum class oluştur aşağıdaki yapıya uygun olarak.\n" +
            "ben bir metoda \"java\" diye parametre yolladığımda bana \"JavaLexer\" değerini getirebilmeli\n" +
            "başka bir metoda \"java\" diye parametre yolladığımda bana \"pygments.lexers.jvm.JavaLexer\" değerini getirebilmeli:\n" +
            "\n" +
            "JAVA_LEXER (\"java\", \"JavaLexer\", \"pygments.lexers.jvm.JavaLexer\"),\n" +
            "ADA_LEXER (\"ada\", \"AdaLexer\", \"pygments.lexers.jvm.AdaLexer\"),\n" +
            "\n" +
            "\n" +
            "birinci değişkenin adı languageString, ikincisi languageClassName, üçüncüsü languageClassFullPath olsun.\n" +
            "\n" +
            "CEVAP:\n" +
            "\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "500den fazla enum değerini ben birleştirip aşağıdaki koda yapıştıracağım, kod çalışıyor.\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "Notepad++ ve Libreoffice Calc ı açıp verileri enum yapacak şekilde onunla düzenliyorum.\n" +
            "\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "tablardan kurtulalım:\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "Tırnak işaretlerini süslü yaptığı için onları da \" ile bul değiştir yaptıktan sonra koda yapıştırınca çalışıyor.\n" +
            "\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "</rich_text><rich_text justification=\"left\"></rich_text><rich_text>\n" +
            "\n" +
            "Duplicate veri bir tane var. Orijinal dökümanda(</rich_text><rich_text link=\"webs https://pygments.org/docs/lexers/\">https://pygments.org/docs/lexers/</rich_text><rich_text>) öyle gibi, o satırı sildim.\n" +
            "Niye iki tane oldu çünkü websayfasını komple kopyalamıştım, orada duplicate veri yok ama bir tanesini sayfanın üstünde örnekte kullandığı için öyle oldu.\n" +
            "\n" +
            "\n" +
            "\n" +
            "</rich_text></node>\n";


}
