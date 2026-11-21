package com.turkerozturk.richtext.editing;

import java.util.*;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class EditorComfortTemplateTest {
    private org.jsoup.nodes.Document render(String template, String syntax, String language) {
        var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
        var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath("/app");request.setRequestURI("/app/nodes/content/3");
        var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(language));
        var node=new Node();node.setNodeId(3);node.setName("<literal>");node.setTxt("Text\n😀");node.setSyntax(syntax);context.setVariable("node",node);context.setVariable("tenantViewToken","current");context.setVariable("editorRecoveryScope","scope");
        context.setVariable("editor",Map.of("node",node,"document",Map.of("runs",List.of()),"revision","r","objects",Map.of(),"maxEmbeddedFileBytes",9000000));
        String html=engine.process(template,context);assertThat(html).doesNotContain("??editor.","??view.","<literal>");return org.jsoup.Jsoup.parse(html);
    }
    @Test void everyContentEditorHasTranslatedContextAwareZoomAfterCancelAndBeforeWriting() {
        for(String language:List.of("en","tr"))for(String syntax:List.of("plain-text","python","markdown","html","custom-colors")) {
            boolean rich=syntax.equals("custom-colors");var doc=render(rich?"node/richtext-edit":"node/nodeContentEdit",syntax,language);
            var bar=doc.selectFirst(rich?".rich-savebar":".plain-savebar");var zoom=bar.selectFirst(".editor-zoom-controls");
            assertThat(zoom.previousElementSibling().tagName()).isEqualTo("a");assertThat(zoom.select("button").eachAttr("type")).containsOnly("button");
            assertThat(zoom.select("button").eachAttr("data-editor-zoom")).containsExactly("out","reset","in");assertThat(zoom.selectFirst("[data-editor-zoom-value]").text()).isEqualTo("100%");
            assertThat(zoom.attr("aria-label")).isEqualTo(language.equals("tr")?"Editör metnini yakınlaştır":"Editor text zoom");
            assertThat(doc.select("script[src='/app/js/node/editorComfort.js']")).hasSize(1);
            assertThat(doc.select("link[href='/app/css/node/editorComfort.css']")).hasSize(1);
        }
    }
    @Test void richToolsKeepHistoryParagraphAndObjectSafetyControlsGroupedInRequestedOrder() {
        for(String language:List.of("en","tr")) {
            var doc=render("node/richtext-edit","custom-colors",language);
            assertThat(doc.select(".rich-mode-row #richVisualMode,.rich-mode-row #richUndo,.rich-mode-row #richRedo").eachAttr("id")).containsExactly("richVisualMode","richUndo","richRedo");
            assertThat(doc.select(".rich-paragraph-row [data-choice=scale]")).hasSize(1);assertThat(doc.select("[data-choice=justification]")).isEmpty();
            assertThat(doc.select(".rich-paragraph-row [data-align]").eachAttr("data-align")).containsExactly("left","center","right","fill");
            assertThat(doc.select(".rich-object-safety #richObjects,.rich-object-safety #richAllowDeletion")).hasSize(2);
            assertThat(doc.selectFirst(".rich-object-safety").nextElementSibling().id()).isEqualTo("richDismissLink");
            assertThat(doc.selectFirst("#richControls").children().last().attr("role")).isEqualTo("note");
            assertThat(doc.selectFirst("#richAllowDeletion").hasAttr("checked")).isFalse();
        }
    }
}
