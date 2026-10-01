package com.turkerozturk.richtext.experimental;

import java.util.List;
import java.util.Map;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.turkerozturk.richtext.experimental.RichTextLayout.*;

class RichTextObjectHtmlRendererTest {
    private final RichTextObjectHtmlRenderer renderer = new RichTextObjectHtmlRenderer();
    private final EmbeddedObject image = new EmbeddedObject(ObjectKind.IMAGE, 53, 1);
    private RichTextLayout layout(EmbeddedObject ref) { return new RichTextLayout(List.of(new ObjectPart(ref))); }

    @Test void rendersImageInSplitFormattedTextWithCorrectMimeType() {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aGN8AAAAASUVORK5CYII=");
        var document = new RichTextXmlReader().read("<node><rich_text weight='heavy'>AB</rich_text></node>");
        var layout = new RichTextLayoutBuilder().build(document, List.of(image));
        String html = renderer.render(layout, Map.of(image, new EmbeddedContent.PngImage(png, "", "left")), null);
        assertThat(html).contains("data:image/png;base64,", "max-width:100%", "font-weight:bold");
        assertThat(html.indexOf(">A</span>")).isLessThan(html.indexOf("<img"));
        assertThat(html.indexOf("<img")).isLessThan(html.indexOf(">B</span>"));
    }

    @Test void guardsDownloadLinksAndEscapesFilenames() {
        var ref = new EmbeddedObject(ObjectKind.ATTACHMENT, 53, 2);
        var payload = Map.<EmbeddedObject, EmbeddedContent>of(ref, new EmbeddedContent.Attachment("<file>.txt"));
        assertThat(renderer.render(layout(ref), payload, null)).contains("&lt;file&gt;.txt").doesNotContain("href=");
        assertThat(renderer.render(layout(ref), payload, "token&value")).contains("/download/53/2?_tenantView=token%26value").doesNotContain("<file>");
    }

    @Test void escapesAnchorIdentifiersAndCodeWithoutInterpretingHtml() {
        var anchor = new EmbeddedObject(ObjectKind.ANCHOR, 53, 0);
        var code = new EmbeddedObject(ObjectKind.CODEBOX, 53, 1);
        var layout = new RichTextLayout(List.of(new ObjectPart(anchor), new ObjectPart(code)));
        String html = renderer.render(layout, Map.of(anchor, new EmbeddedContent.Anchor("x\" onclick=\"bad"),
                code, new EmbeddedContent.CodeBox("<script>\n\ttext", "java\" onload=\"bad")), null);
        assertThat(html).contains("x&quot; onclick=&quot;bad", "&lt;script&gt;\n\ttext", "overflow-x:auto").doesNotContain("<script>", "onclick=\"bad", "onload=\"bad");
    }

    @Test void rendersStorageHeaderLastAsVisualHeaderFirst() {
        var ref = new EmbeddedObject(ObjectKind.TABLE, 53, 0);
        var table = new EmbeddedContent.Table(List.of(List.of("<body>", ""), List.of("Heading", "Other")), Map.of());
        String html = renderer.render(layout(ref), Map.of(ref, table), null);
        assertThat(html).contains("<thead><tr><th>Heading</th><th>Other</th></tr></thead>", "<td>&lt;body&gt;</td><td></td>", "overflow-x:auto");
        assertThat(table.rows().get(0).get(0)).isEqualTo("<body>");
    }

    @Test void rejectsMissingMismatchedAndNonPngPayloads() {
        assertThatThrownBy(() -> renderer.render(layout(image), Map.of(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> renderer.render(layout(image), Map.of(image, new EmbeddedContent.Anchor("x")), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> renderer.render(layout(image), Map.of(image, new EmbeddedContent.PngImage(new byte[]{1}, "", "")), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void copiesImageBytesInBothDirections() {
        byte[] bytes = {1, 2};
        var image = new EmbeddedContent.PngImage(bytes, "", "");
        bytes[0] = 9;
        image.bytes()[1] = 9;
        assertThat(image.bytes()).containsExactly((byte)1, (byte)2);
    }
}
