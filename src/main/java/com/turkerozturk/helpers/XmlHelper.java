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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XmlHelper {

    /**
     * Bu metodu sadece CherryTree Node syntax alaninda custom-colors yazili olan node lerin txt alaninda rastladigim
     * bir xml yapisini html yapisina cevirmek icin saatlerce ugrasarak gelistirdim.
     * Zannedersem emin olamasam da yapi gtk pango diye geciyor ve java kutuphansei herhalde yoktur.
     * Daha yapilmasi gereken islemler var fakat en azindan icerik metinlerinin alt satira gecip gecmemesi ve her
     * satir baslangicindan itibaren bosluk karakteri sayisinin ayni olmasi saglandi.
     * Dolayisiyla gorunus olarak CherryTree programindaki ile ayni konumlandirmaya sahip oldu.
     * En distaki elemetn haricinde tum elementler span cunku. Rolleri farkli. Bir de iclerindeki enter karakterlerini
     * br elementi ile yerdegistirdim.
     * Eksik olan seyler ise, attributelarin hepsini data- dan sonra attribute ismi bicimine donusturdum.
     * O datalari okuyup, elementleri linke, basliklara ceviren, on ve arkaplan renklerini, fontlarini hizalarini
     * belirleyen, linklerin internal mi external mi olduklarina gore islem yapan metodlar yazmak.
     * Bunlar belki javascript ile de yapilabilir.
     * @param xmlContent
     * @return
     */
    public String convertXmlToHtml(String xmlContent) {
        xmlContent = replaceEnterWithBr(xmlContent);
        xmlContent = replaceSpacesAfterBrWithNonBreakingSpace(xmlContent);

        // https://www.baeldung.com/spring-classpath-file-access
        // Relatif dosya yolundan yukleyebilmek icin iki saat ugrastim sonunda yukaridaki linkten buldum.
        // Cok sey denedim oncesinde, bean, servis, properties, @value falan, ugrasma tekrar bunlarla zamanin gider.
        // Ikinci kez tekrar ugrastim, development ortamindaki compiled jar icinden de xslt yi bulup okuyup
        // calisabilsin diye.
        // https://stackoverflow.com/questions/44399422/read-file-from-resources-folder-in-spring-boot/54225276#54225276
        // Hem jar hem de development ortaminda problemsiz calismasi icin boyle yapildi.

        String xsltContent = getResourceFileAsString("static/xslt/transform.xslt");

        String transformedXml = transformXML(xmlContent, xsltContent);
        return transformedXml;
    }

    public String convertXmlTableToHtml(String xmlContent) {
        xmlContent = replaceEnterWithBr(xmlContent);
        xmlContent = replaceSpacesAfterBrWithNonBreakingSpace(xmlContent);

        String xsltContent = getResourceFileAsString("static/xslt/transformtable.xslt");

        String transformedXml = transformXML(xmlContent, xsltContent);
        return transformedXml;
    }

    public String transformXML(String xmlContent, String xsltContent) {
        try {
            // Transformer olustur
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(new StringReader(xsltContent)));

            // Ciktiyi yazmak icin StringWriter olustur
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            // Transform islemi
            transformer.transform(new StreamSource(new StringReader(xmlContent)), result);

            // StringWriter icerigini string degiskenine yukle
            String transformedXml = writer.toString();

            // StringWriter kapat
            writer.close();

            // Donuşturulmus XML'i geri dondur
            return transformedXml;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * CherryTree pango xml yapisindan baska yerde gormedigim, #FFFFCCCC9999 tipindeki duplicate yazilmis renk bilgisini
     * # karakteri dahil 13 karakterli renk kodlamasini #FFCC99 tipindeki bildigimiz renk gosterimine donusturur.
     * @param color
     * @return
     */
    public String convertColor(String color) {
        if (color.length() == 13 && color.charAt(0) == '#') {
            String sonuc = "#" + color.substring(1, 3) + color.substring(5, 7) + color.substring(9,11);
            return sonuc;
        }
        return color;
    }

    /*
    Asagida yeni model metodlar var
    once replaceEnterWithBr, sonra replaceSpacesAfterBrWithNonBreakingSpace metodu calistirilmalidir.
     */

    /**
     * enter karakterleri yerine br elementleri yerlestiren metod.
     * CherryTree yazilimina ozgu zengin metin bicimli xml yapisi(belki de gtk pango yapisidir), rich_text elementleri
     * icindeki yeni satir karakterlerini dikkate alir.
     * Bu metodun gorevi o enter karakterlerini br elementi ile degistirmek. Boylece html gorunumunde yeni satira
     * inmeyi saglamak. Cunku bu yapidaki rich_text elementleri bir cesit span html elementi gibi davraniyor.
     * Ama iclerinde enter olunca alt satira inebilen bir cozum olarak br elementi kullanmak gerekiyor.
     * Diger yandan, raw xml icerikteki tum enter karakterlerini degil, sadece rich_text elementlerinin icindekileri
     * br elementi ile degistirmek gerekiyor. Bu metodun gorevi bu islemleri yerine getirmek.
     * @param metin
     * @return
     */
    public String replaceEnterWithBr(String metin) {
        // Pattern olustur
        Pattern pattern = Pattern.compile("<rich_text>(.*?)</rich_text>", Pattern.DOTALL);

        // Matcher olustur
        Matcher matcher = pattern.matcher(metin);

        // Replace islemi yap
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String icerik = matcher.group(1);
            // Enter karakterlerini <br/> ile degistir
            String yeniIcerik = icerik;
            try {
                yeniIcerik = icerik.replaceAll("\\r?\\n", "<br/>");
                // Text icerigindeki ozel karakterlerin kacis dizileriyle islenmesi
                String quotedYeniIcerik = Matcher.quoteReplacement(yeniIcerik);
                matcher.appendReplacement(sb, "<rich_text>" + quotedYeniIcerik + "</rich_text>");
            } catch (IllegalArgumentException illegalArgumentException) {
                // Bu hataya sebep olan sey regex matchere gelen metnin icinde regexte kullanilan ozel karakterler
                // varsa olmasiydi. Matcher.quoteReplacement kullaninca problem cozuldu. TODO Ancak icerigin eksiksiz
                // gorounup gorunmediginin frontend tarafinda kontrolunu yapmak gerekir.
                System.out.println("PROGLEM IllegalArgumentException ********************************************\n\n");
                System.out.println("\""+ yeniIcerik + "\"");
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * enter karakterleri yerine br elementleri yerlestiren metoddan sonra calistirilmasi gerekir.
     * CherryTree yazilimina ozgu zengin metin bicimli xml yapisi(belki de gtk pango yapisidir), rich_text elementleri
     * icindeki her satirdan itibaren soldan ilk karaktere kadar olan bosluk karakterlerini dikkate alir.
     * Bu metodun gorevi o bosluk karakterlerini non breaking space yaparak html gosterimi esnasinda yutulmasini
     * onleyerek orijinal programdaki gibi dikkate alinmasini gosterilmesini saglamak.
     * xslt donusum islemlerinde kutuphane tarafli bir non breaking space hatasini gidermek icin de gerekli bul degistir
     * islemi yapilarak onlem alindi. Buna ragmen bir hata olursa matcher.appendReplacement metodunda regex escape
     * etmedigimiz icin olabilir. Ustteki enter karakterini br elementi ile replace eden metoda bakara oradaki onlem
     * alinabilir.
     * @param metin
     * @return
     */
    public String replaceSpacesAfterBrWithNonBreakingSpace(String metin) {
        // Pattern oluştur
        Pattern pattern = Pattern.compile("<br\\/>\\s+");

        // Matcher oluştur
        Matcher matcher = pattern.matcher(metin);

        // Replace işlemi yap
        StringBuffer yeniMetin = new StringBuffer();
        while (matcher.find()) {
            // <br/> etiketi ile birlikte gelen bosluk sayisini bul
            int spaceCount = matcher.group().length() - 5; // "<br/>" uzunlugu 5 karakter

            // Replace islemi yap
            String replacement = "<br/>";
            for (int i = 0; i < spaceCount; i++) {
                // replacement += "&nbsp;";
                replacement += "&#xa0;";
            }
            matcher.appendReplacement(yeniMetin, replacement);
        }
        matcher.appendTail(yeniMetin);

        return yeniMetin.toString();
    }

    public static String getResourceFileAsString(String fileName) {
        InputStream is = getResourceFileAsInputStream(fileName);
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return (String)reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } else {
            throw new RuntimeException("resource not found");
        }
    }

    public static InputStream getResourceFileAsInputStream(String fileName) {
        ClassLoader classLoader = XmlHelper.class.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

}
