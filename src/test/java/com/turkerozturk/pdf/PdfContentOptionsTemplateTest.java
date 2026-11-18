package com.turkerozturk.pdf;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class PdfContentOptionsTemplateTest {
    @Test void bothFormsRenderLocalizedDefaultsAndRememberedChoices() {
        for (var locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"))) {
            var engine = new SpringTemplateEngine(); var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
            engine.setTemplateResolver(resolver);
            var messages = new ResourceBundleMessageSource(); messages.setBasename("messages"); messages.setDefaultEncoding("UTF-8");
            engine.setTemplateEngineMessageSource(messages);
            var servlet = new MockServletContext();
            var context = new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(
                    new MockHttpServletRequest(servlet), new MockHttpServletResponse()), locale);
            context.setVariable("nodeId", 1L); context.setVariable("nodeName", "Host");
            context.setVariable("tenantViewToken", "current");
            context.setVariable("objects", com.turkerozturk.richtext.experimental.PdfObjectSelection.all());
            for (boolean enabled : new boolean[]{false, true}) {
                context.setVariable("includeDescendants", enabled); context.setVariable("contents", false);
                context.setVariable("options", new PdfExportOptions("A4", "portrait", true, true, true,
                        false, false, false, false, false, enabled, enabled));
                var html = engine.process("node/pdfExportOptions", context);
                assertThat(html).doesNotContain("??");
                var doc = org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("select[name=inlineLinks] option[selected]").val()).isEqualTo("" + enabled);
                assertThat(doc.selectFirst("select[name=newPagePerNode] option[selected]").val()).isEqualTo("" + enabled);
                assertThat(doc.selectFirst("form").attr("action")).isEqualTo(enabled ? "/nodes/pdf/1/subtree" : "/nodes/pdf/1");
            }
        }
    }
}
