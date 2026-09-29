package com.turkerozturk.helpers.highlighter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class CodeHighLighterTest {
    private final CodeHighLighter highlighter = new CodeHighLighter();

    @AfterEach void disableHighlighting() {
        highlighter.setSyntaxHighlightingEnabled(false);
    }

    @Test void escapesMarkupWhenDisabled() {
        highlighter.setSyntaxHighlightingEnabled(false);
        String html = CodeHighLighter.highlightLanguage("java", "<script>alert(1)</script> & 'test'");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;").contains("&amp;");
        assertThat(html).doesNotContain("<script>").doesNotContain("cherry-highlight");
    }

    @Test void usesOnlyBundledLanguagesAndResolvesCherryTreeAliases() {
        highlighter.setSyntaxHighlightingEnabled(true);
        assertThat(CodeHighLighter.languageFor("sh")).isEqualTo("bash");
        assertThat(CodeHighLighter.languageFor("dosbatch")).isEqualTo("dos");
        assertThat(CodeHighLighter.languageFor("js")).isEqualTo("javascript");
        assertThat(CodeHighLighter.languageFor("python3")).isEqualTo("python");
        assertThat(CodeHighLighter.languageFor("unknown\" onclick=\"alert(1)")).isNull();
        assertThat(CodeHighLighter.highlightLanguage("java", "if (x < y) {}"))
                .contains("data-language=\"java\"")
                .contains("x &lt; y");
        assertThat(CodeHighLighter.highlightLanguage("unknown\" onclick=\"alert(1)", "<b>"))
                .doesNotContain("data-language", "onclick", "<b>")
                .contains("&lt;b&gt;");
    }

    @Test void highlightedCodeMarkupKeepsSpecialCharactersAsText() throws Exception {
        highlighter.setSyntaxHighlightingEnabled(true);
        String original = "<tag attr=\"x\"> & <script>alert(1)</script>";
        String markup = CodeHighLighter.mappedhighlightLanguage("xml", original);

        var parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var document = parser.parse(new InputSource(new StringReader(markup)));

        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("pre");
        assertThat(document.getElementsByTagName("script").getLength()).isZero();
        assertThat(document.getElementsByTagName("code").item(0).getTextContent()).isEqualTo(original);
    }

    @Test void syntaxPickerOnlyOffersBundledGrammarsAndKnownAliases() {
        assertThat(CodeHighLighter.supportedSyntaxes()).contains("java", "sh", "dosbatch", "python3")
                .doesNotContain("custom-colors", "plain-text", "plaintext");
        assertThat(CodeHighLighter.supportsCodeSyntax("java")).isTrue();
        assertThat(CodeHighLighter.supportsCodeSyntax("dosbatch")).isTrue();
        assertThat(CodeHighLighter.supportsCodeSyntax("<script>")).isFalse();
    }
}
