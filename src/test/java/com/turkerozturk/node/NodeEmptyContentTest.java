package com.turkerozturk.node;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class NodeEmptyContentTest {
    @Test void plainAndCodeWhitespaceAreEmptyButLiteralMarkupIsContent() {
        var node=new Node();node.setSyntax("plain-text");assertThat(node.isContentEmpty()).isTrue();node.setTxt(" \n\t");assertThat(node.isContentEmpty()).isTrue();node.setTxt("<p></p>");assertThat(node.isContentEmpty()).isFalse();node.setSyntax("java");node.setTxt("");assertThat(node.isContentEmpty()).isTrue();
    }
    @Test void emptyRichMarkupIsEmptyWhileObjectsAndTextRemainVisible() {
        var node=new Node();node.setSyntax("custom-colors");node.setTxt("<node><rich_text> </rich_text></node>");assertThat(node.isContentEmpty()).isTrue();
        node.setTxtAsHtml("<p><br>&nbsp;</p>");assertThat(node.isContentEmpty()).isTrue();
        for(String html:new String[]{"<p>Hello</p>","<img src='/image'>","<table><tr><td></td></tr></table>","<pre></pre>","<a>📎 file</a>"}){node.setTxtAsHtml(html);assertThat(node.isContentEmpty()).as(html).isFalse();}
    }
}
