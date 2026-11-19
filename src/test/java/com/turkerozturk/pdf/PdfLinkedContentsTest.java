package com.turkerozturk.pdf;

import com.turkerozturk.node.fragments.NodeFragmentService;
import com.turkerozturk.node.fragments.NodeFragmentSettings;
import com.turkerozturk.richtext.experimental.PdfObjectSelection;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdfLinkedContentsTest {
    private final NodeFragmentService fragments = mock(NodeFragmentService.class);
    private final PdfObjectSelection objects = new PdfObjectSelection(false, true, false, false);
    private NodeFragmentSettings settings(int depth, int count) {
        return new NodeFragmentSettings(depth, count, 250000, 1048576, 16777216, 200);
    }
    private PdfLinkedContents expander(int depth, int count) {
        return new PdfLinkedContents(fragments, settings(depth, count), new PdfExportLimits(), objects);
    }
    private void target(long occurrence, long content, String syntax, String html) {
        when(fragments.fragmentForPdf(occurrence, objects)).thenReturn(
                new NodeFragmentService.Fragment("" + content, "Source " + content, syntax, html));
    }
    @Test void repeatedSourcesHaveIndependentAnchorsAndKeepSurroundingText() throws Exception {
        target(2, 2, "custom-colors", "<div id='a'>Included text<a href='/nodes/2#a'>local</a></div>");
        var html = expander(3, 20).expand("<p>Before <a href='/nodes/2'>one</a> between <a href='/nodes/2'>two</a> after</p>", 1);
        var parsed = Jsoup.parseBodyFragment(html);
        assertThat(parsed.select(".pdf-linked-content")).hasSize(2);
        assertThat(parsed.select("[id]").eachAttr("id")).containsExactly("sc-fragment-1-a", "sc-fragment-2-a");
        assertThat(parsed.select("a").eachAttr("href")).containsExactly("#sc-fragment-1-a", "#sc-fragment-2-a");
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Host", html, 1)))) {
            assertThat(new PDFTextStripper().getText(pdf)).contains("Before", "between", "after", "Included text").doesNotContain("Source 2");
        }
    }
    @Test void inlineFileNameStartsAtLinkPositionWithoutSourceLabelOrArtificialBreaks() throws Exception {
        target(2, 2, "custom-colors", "<div><strong>sweetcherry_1.11.0_all_os_portable.zip</strong></div>");
        var html = expander(3, 20).expand("<div>Get-FileHash -Algorithm SHA256 .\\<a href='/nodes/2'>version variable</a> suffix</div>", 1);
        var parsed = Jsoup.parseBodyFragment(html);
        assertThat(parsed.select(".pdf-linked-content").first().tagName()).isEqualTo("span");
        assertThat(parsed.select("br")).isEmpty();
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Host", html, 1)))) {
            var text = new PDFTextStripper(); text.setSortByPosition(true);
            assertThat(text.getText(pdf))
                    .contains("Get-FileHash -Algorithm SHA256 .\\sweetcherry_1.11.0_all_os_portable.zip suffix")
                    .doesNotContain("Source 2", "version variable");
        }
    }
    @Test void multilineFragmentsKeepEmptyLinesNestedInclusionsAndInlineEdges() throws Exception {
        target(2, 2, "custom-colors", "<div>first<a href='/nodes/3'>nested</a></div>\n<div><br></div><div>last</div>");
        target(3, 3, "custom-colors", "<p>INNER</p>");
        var html = expander(3, 20).expand("<div>prefix<a href='/nodes/2'>target</a>suffix</div>", 1);
        assertThat(Jsoup.parseBodyFragment(html).select("br")).hasSize(2);
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Host", html, 1)))) {
            var text = new PDFTextStripper(); text.setSortByPosition(true);
            assertThat(text.getText(pdf)).contains("prefixfirstINNER", "lastsuffix")
                    .doesNotContain("Source 2", "Source 3");
        }
    }
    @Test void cyclesIncludingAliasesAndUnavailableTargetsKeepTheirLabels() {
        target(2, 2, "custom-colors", "<div>Included<a href='/nodes/3'>alias back</a></div>");
        target(3, 1, "custom-colors", "must never include");
        when(fragments.fragmentForPdf(4, objects)).thenThrow(new NodeFragmentService.Problem("managed"));
        var html = expander(3, 20).expand("<a href='/nodes/2'>start</a><a href='/nodes/4'>hidden label</a>", 1);
        assertThat(html).contains("Included", "alias back", "hidden label").doesNotContain("must never include");
        assertThat(Jsoup.parseBodyFragment(html).select(".pdf-linked-content")).hasSize(1);
    }
    @Test void plainTextNewlinesAndIntentionalLeadingEmptyLinesSurvive() throws Exception {
        target(2, 2, "plain-text", "<div>first\nsecond</div>");
        target(3, 3, "custom-colors", "<div><br></div><div>after blank</div>");
        var html = expander(3, 20).expand("<div>prefix<a href='/nodes/2'>plain</a>suffix</div><div><a href='/nodes/3'>rich</a></div>", 1);
        assertThat(Jsoup.parseBodyFragment(html).select(".pdf-linked-content").get(1).select("br")).hasSize(1);
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Host", html, 1)))) {
            var text = new PDFTextStripper(); text.setSortByPosition(true);
            assertThat(text.getText(pdf)).contains("prefixfirst\nsecondsuffix", "after blank");
        }
    }
    @Test void includedHeadingsKeepTheirOutlineDestinationWithoutAddingSourceHeadings() throws Exception {
        target(2, 2, "custom-colors", "<div><span data-pdf-heading='h2'>Included heading</span></div><div>body</div>");
        var html = expander(3, 20).expand("<a href='/nodes/2'>source label</a>", 1);
        try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Host", html, 1)))) {
            var heading = pdf.getDocumentCatalog().getDocumentOutline().getFirstChild().getFirstChild();
            assertThat(heading.getTitle()).isEqualTo("Included heading");
            assertThat(heading.findDestinationPage(pdf)).isEqualTo(pdf.getPage(0));
            assertThat(new PDFTextStripper().getText(pdf)).doesNotContain("Source 2", "source label");
        }
    }
    @Test void depthAndCountAreSharedAcrossSubtreeRoots() {
        target(2, 2, "custom-colors", "<a href='/nodes/3'>deeper</a>");
        var exporter = expander(1, 1);
        assertThat(exporter.expand("<a href='/nodes/2'>source</a>", 1)).contains("deeper");
        assertThat(exporter.expand("<a href='/nodes/2'>second root</a>", 5)).contains("second root").doesNotContain("pdf-linked-content");
        verify(fragments, times(1)).fragmentForPdf(2, objects);
        verify(fragments, never()).fragmentForPdf(3, objects);
    }
    @Test void sameNodeAnchorsExternalLinksAndTerminalSyntaxAreNotExpanded() {
        target(2, 2, "html", "<a href='/nodes/3'>terminal</a>");
        var html = expander(3, 20).expand("<a href='/nodes/1#a'>own anchor</a><a href='https://example.org/nodes/2'>external</a><a href='//example.org/nodes/2'>external2</a><a href='/nodes/2'>target</a>", 1);
        assertThat(html).contains("own anchor", "https://example.org/nodes/2", "terminal");
        verify(fragments, times(1)).fragmentForPdf(2, objects);
        verifyNoMoreInteractions(fragments);
    }
    @Test void totalHtmlBudgetLeavesOversizedTargetAsText() {
        target(2, 2, "plain-text", "x".repeat(200));
        var limits = new PdfExportLimits(); limits.setMaxHtmlCharacters(100);
        var exporter = new PdfLinkedContents(fragments, settings(3, 20), limits, objects);
        assertThat(exporter.expand("<a href='/nodes/2'>oversized</a>", 1)).contains("oversized").doesNotContain("pdf-linked-content");
    }
    @Test void fragmentPageBreakIsOptionalAndPdfRetainsEmbeddedObjects() throws Exception {
        var image = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "png", bytes);
        target(2, 2, "custom-colors", "<div>Included<table><tr><td>cell</td></tr></table><pre>codebox</pre><img src='data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()) + "'></div>");
        for (boolean newPage : new boolean[]{false, true}) {
            var options = new PdfExportOptions("A4", "portrait", true, true, true, false, false, false, false, false, true, newPage);
            var html = expander(3, 20).expand("<div>Host text<a href='/nodes/2'>target</a></div>", 1);
            try (var pdf = Loader.loadPDF(new NodePdfRenderer().render(new NodePdfDocument().prepare("Host", html, 1, options, null)))) {
                assertThat(pdf.getNumberOfPages()).isEqualTo(newPage ? 2 : 1);
                assertThat(new PDFTextStripper().getText(pdf)).contains("Host text", "Included", "cell", "codebox");
                assertThat(pdf.getPage(pdf.getNumberOfPages() - 1).getResources().getXObjectNames()).isNotEmpty();
            }
        }
    }
}
