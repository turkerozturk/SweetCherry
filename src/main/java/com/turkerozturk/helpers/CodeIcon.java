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

    OTHER("other", NodeIcon.CT_CODE, ""), // ct_code.svg (like in original)

    /**  original CherryTree mappings below: **/

    C("c", NodeIcon.CT_C, "c"),
    C_SHARP("c-sharp", NodeIcon.CT_CSHARP, "cs"),
    CMAKE("cmake", NodeIcon.CT_CMAKE, "cmake"),
    CPP("cpp", NodeIcon.CT_CPP, "cpp"),
    CSS("css", NodeIcon.CT_CSS, "css"),
    CSV("csv", NodeIcon.CT_CSV, "csv"),
    DIFF("diff", NodeIcon.CT_DIFF, "diff"),
    DOSBATCH("dosbatch", NodeIcon.CT_TERM_RED, "bat"),
    GO("go", NodeIcon.CT_GO, "go"),
    GTK_DOC("gtk-doc", NodeIcon.CT_GTK, ""),
    GTKRC("gtkrc", NodeIcon.CT_GTK, "gtkrc"),
    HTML("html", NodeIcon.CT_HTML, "html"),
    INI("ini", NodeIcon.CT_INI, "ini"),
    JAVA("java", NodeIcon.CT_JAVA, "java"),
    JS("js", NodeIcon.CT_JS, "js"),
    JSON("json", NodeIcon.CT_JSON, "json"),
    LATEX("latex", NodeIcon.CT_LATEX, "tex"),
    LUA("lua", NodeIcon.CT_LUA, "lua"),
    MARKDOWN("markdown", NodeIcon.CT_MARKDOWN, "md"),
    MARKDOWN_EXTRA("markdown-extra",NodeIcon.CT_MARKDOWN, ""),
    MATLAB("matlab", NodeIcon.CT_MATLAB, "m"),
    MESON("meson", NodeIcon.CT_MESON, "meson"),
    PERL("perl", NodeIcon.CT_PERL, "pl"),
    PHP("php", NodeIcon.CT_PHP, "php"),
    POWERSHELL("powershell", NodeIcon.CT_TERM_RED, "ps1"),
    PYTHON("python", NodeIcon.CT_PYTHON, "py"),
    PYTHON3("python3", NodeIcon.CT_PYTHON, ""),
    RUBY("ruby", NodeIcon.CT_RUBY, "rb"),
    RUST("rust", NodeIcon.CT_RUST, "rs"),
    SCALA("scala", NodeIcon.CT_SCALA, "scala"),
    SH("sh", NodeIcon.CT_TERM, "sh"),
    SQL("sql", NodeIcon.CT_DB, "sql"),
    SWIFT("swift", NodeIcon.CT_SWIFT, "swift"),
    XML("xml", NodeIcon.CT_XML, "xml"),
    YAML("yaml", NodeIcon.CT_YAML, "yaml");

    private String syntax;
    private NodeIcon nodeIcon;
    private final String fileExtension;

    CodeIcon(String syntax, NodeIcon nodeIcon, String fileExtension) {
        this.fileExtension = fileExtension;
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

    /** A single canonical mapping: .md selects Markdown and .py selects Python, not their variants. */
    public String getFileExtension() { return fileExtension; }
    public static java.util.Optional<CodeIcon> findByFileExtension(String extension) {
        if (extension == null) return java.util.Optional.empty();
        String key=extension.toLowerCase(java.util.Locale.ROOT);
        if(key.startsWith("."))key=key.substring(1);
        return java.util.Optional.ofNullable(EXTENSIONS.get(key));
    }
    public static CodeIcon getCodeIconByFileExtension(String extension) {
        return findByFileExtension(extension).orElse(OTHER);
    }
    private static final Map<String,CodeIcon> EXTENSIONS=new HashMap<>();
    static {
        for(var item:values()) if(!item.fileExtension.isEmpty()) {
            if(EXTENSIONS.put(item.fileExtension,item)!=null)throw new IllegalStateException("Duplicate extension");
        }
        EXTENSIONS.put("h",C);EXTENSIONS.put("cc",CPP);EXTENSIONS.put("cxx",CPP);EXTENSIONS.put("hpp",CPP);
        EXTENSIONS.put("htm",HTML);EXTENSIONS.put("yml",YAML);EXTENSIONS.put("cmd",DOSBATCH);
        EXTENSIONS.put("patch",DIFF);EXTENSIONS.put("markdown",MARKDOWN);EXTENSIONS.put("bash",SH);
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
