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

import java.util.HashMap;
import java.util.Map;

public enum CodeIcon {

    OTHER("other", NodeIcon.CT_CODE), // ct_code.svg (like in original)

    /**  original CherryTree mappings below: **/

    C("c", NodeIcon.CT_C),
    C_SHARP("c-sharp", NodeIcon.CT_CSHARP),
    CMAKE("cmake", NodeIcon.CT_CMAKE),
    CPP("cpp", NodeIcon.CT_CPP),
    CSS("css", NodeIcon.CT_CSS),
    CSV("csv", NodeIcon.CT_CSV),
    DIFF("diff", NodeIcon.CT_DIFF),
    DOSBATCH("dosbatch", NodeIcon.CT_TERM_RED),
    GO("go", NodeIcon.CT_GO),
    GTK_DOC("gtk-doc", NodeIcon.CT_GTK),
    GTKRC("gtkrc", NodeIcon.CT_GTK),
    HTML("html", NodeIcon.CT_HTML),
    INI("ini", NodeIcon.CT_INI),
    JAVA("java", NodeIcon.CT_JAVA),
    JS("js", NodeIcon.CT_JS),
    JSON("json", NodeIcon.CT_JSON),
    LATEX("latex", NodeIcon.CT_LATEX),
    LUA("lua", NodeIcon.CT_LUA),
    MARKDOWN("markdown", NodeIcon.CT_MARKDOWN),
    MARKDOWN_EXTRA("markdown-extra",NodeIcon.CT_MARKDOWN),
    MATLAB("matlab", NodeIcon.CT_MATLAB),
    MESON("meson", NodeIcon.CT_MESON),
    PERL("perl", NodeIcon.CT_PERL),
    PHP("php", NodeIcon.CT_PHP),
    POWERSHELL("powershell", NodeIcon.CT_TERM_RED),
    PYTHON("python", NodeIcon.CT_PYTHON),
    PYTHON3("python3", NodeIcon.CT_PYTHON),
    RUBY("ruby", NodeIcon.CT_RUBY),
    RUST("rust", NodeIcon.CT_RUST),
    SCALA("scala", NodeIcon.CT_SCALA),
    SH("sh", NodeIcon.CT_TERM),
    SQL("sql", NodeIcon.CT_DB),
    SWIFT("swift", NodeIcon.CT_SWIFT),
    XML("xml", NodeIcon.CT_XML),
    YAML("yaml", NodeIcon.CT_YAML);

    private String syntax;
    private NodeIcon nodeIcon;

    CodeIcon(String syntax, NodeIcon nodeIcon) {
        this.syntax = syntax;
        this.nodeIcon = nodeIcon;
    }

    private static final Map<String, CodeIcon> _map = new HashMap<String, CodeIcon>();
    static {
        for (CodeIcon enumVariableName : CodeIcon.values())
            _map.put(enumVariableName.syntax, enumVariableName);
    }

    public static CodeIcon getCodeIconBySyntax(String syntax) {
        CodeIcon codeIcon = _map.get(syntax);
        return codeIcon != null ? codeIcon : OTHER;
    }

    @Override
    public String toString() {
        return "CodeIcon{" +
                "syntax='" + syntax + '\'' +
                ", nodeIcon=" + nodeIcon +
                '}';
    }

    public String getSyntax() {
        return syntax;
    }

    public NodeIcon getNodeIcon() {
        return nodeIcon;
    }
}
