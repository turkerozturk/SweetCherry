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
package com.turkerozturk.helpers.highlighter.pygments;

import org.apache.commons.lang3.StringEscapeUtils;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;

import java.util.Properties;

public class CodeHighLighter {


    /**
     * Bu metod Jython kutuphanesi sayesinde Pygments kutuphanesini kullanarak yuzlerce kodlama dilini highlight
     * yapabilmeyi sagliyor. CherryTree ile baglantili kullanilacagindan, CherryTree node.syntax sutununda ne
     * yaziyorsa, bu metod onu benim Pygments websitesinden elde ettigim lexer class isimlerinden ilgili olani
     * elde edip highlight yapailmek icin kullanir. Bu sebeple, enum classdaki yuzlerce ogenin ilk parametrelerini,
     * CherryTree'deki node.syntax sutununun alabilecegi string degerlerle ayni yapmak gerekir daha sonra ki
     * eslesme gerceklessin ve highlight islemini yapan satirlar calisabilsin.
     * @param languageName
     * @param code
     * @return
     */
    public static String highlightLanguage(String languageName, String code) {

       // https://stackoverflow.com/questions/65797711/spring-boot-app-with-jython-on-raspberry-pi
       // Properties props = new Properties();
       // props.put("python.home", "/home/pi/Dysk/jython-standalone-2.7.1.jar");
       // props.put("python.console.encoding", "UTF-8");
       // props.put("python.security.respectJavaAccessibility", "false");
       // props.put("python.import.site", "false");
       // Properties preprops = System.getProperties();
       // PythonInterpreter.initialize(preprops, props, new String[0]);

        // Jython sayesinde
        PythonInterpreter interpreter = new PythonInterpreter();

        // Set a variable with the content you want to work with
        interpreter.set("code", code);

        //String lexerName = "JavaLexer";
        String lexerName = LexerEnum.findClassNameByLanguageString(languageName);

        // Simple use Pygments as you would in Python
        interpreter.exec("from pygments import highlight\n"
                + "from pygments.lexers import " + lexerName
                + "\nfrom pygments.formatters import HtmlFormatter\n"
                + "\nformatter = HtmlFormatter(linenos=True, cssclass=\"highlight\")"
                + "\nresult = highlight(code, " + lexerName + "(), formatter)");

        // Get the result that has been set in a variable
        return (interpreter.get("result", String.class));
    }


    public static String mappedhighlightLanguage(String languageName, String code) {
        // duz veya zengin metin olmayan tum icerikler syntax sutununda yazan dile gore highlight edilir.
        // https://pygments.org/docs/lexers/#pygments.lexers.shell.MSDOSSessionLexer
        // adresinde hangi lexer hangi turleri renklendirebiliyor yaziyor.
        // Ornek olarak BashLexer sunlardan fazlasini renklendirir: bash, sh, ksh, zsh, shell, openrc
        String mappedHighlighter = languageName;

        if (languageName.equals("plain-text")) {
            mappedHighlighter = "text";
        } else if (languageName.equals("sh")) {
            mappedHighlighter = "bash";
        } else if (languageName.equals("dosbatch")) {
            mappedHighlighter = "msdossession";
        } else if (languageName.equals("js")) {
            mappedHighlighter = "javascript";
        } else if (languageName.equals("python")) {
            mappedHighlighter = "python2"; // eskide kaldigi icin python yerine python 2 demisler
        } else if (languageName.equals("python3")) {
            mappedHighlighter = "python"; // aciklamasi pygment sitesinde var, 3 icin python diye degistirmisler
        } else {
            mappedHighlighter = languageName;
        }
        String checkIfExist = LexerEnum.findClassNameByLanguageString(mappedHighlighter);




        return highlightLanguage(mappedHighlighter, code);
    }


}
