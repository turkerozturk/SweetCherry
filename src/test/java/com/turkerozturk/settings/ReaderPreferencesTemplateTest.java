package com.turkerozturk.settings;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class ReaderPreferencesTemplateTest {
    @Test void menuColorDialogAndImportControlsFollowApplicationLanguage() {
        for (var locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"))) {
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet)
                    .buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            context.setVariable("_csrf",new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN","_csrf","token"));
            context.setVariable("tenantNames",List.of());context.setVariable("zipUploadBytes",1000000);context.setVariable("zipAttachmentBytes",100);
            context.setVariable("currentTenantName","Demo");context.setVariable("tenantViewToken","current");
            context.setVariable("mmUploadBytes",1000000);context.setVariable("mmMaxNodes",100);
            var menu=engine.process("common/navbarFragment",context);assertThat(menu).doesNotContain("??preferences.");
            var doc=org.jsoup.Jsoup.parse(menu);
            assertThat(doc.select("[data-confirm-toggle]").eachAttr("data-confirm-toggle")).containsExactly("creation","duplication","deletion");
            assertThat(doc.select("[data-node-background-dialog]")).hasSize(1);
            for (var name : List.of("mindmap-import","zip-import")) {
                var html=engine.process(name,context);assertThat(html).doesNotContain("??filePicker.");
                var picker=org.jsoup.Jsoup.parse(html);
                assertThat(picker.selectFirst("[data-file-browse]").text()).isEqualTo(locale.getLanguage().equals("tr")?"Göz at…":"Browse…");
                assertThat(picker.selectFirst("[data-file-name]").text()).isEqualTo(locale.getLanguage().equals("tr")?"Dosya seçilmedi":"No file selected");
                assertThat(picker.selectFirst("input[type=file]").hasAttr("required")).isTrue();
                assertThat(picker.selectFirst("input[type=file]").hasAttr("hidden")).isFalse();
            }
            var node=org.mockito.Mockito.mock(com.turkerozturk.node.Node.class);
            org.mockito.Mockito.when(node.getName()).thenReturn("Node");
            context.setVariable("node",node);context.setVariable("requestedNodeId",9L);context.setVariable("canDeleteNode",true);context.setVariable("view","tree");
            var toolbar=org.jsoup.Jsoup.parse(engine.process("node/nodeToolbarFragment",context));
            assertThat(toolbar.select("form[data-confirm-kind=creation]")).hasSize(2);
            assertThat(toolbar.selectFirst("[data-node-delete]").attr("href")).contains("/nodes/delete/9", "_tenantView=current");
            context.setVariable("selectedNodeId",9L);context.setVariable("sharedNode",false);context.setVariable("deleteView","tree");context.setVariable("trashMode",false);
            var deletion=org.jsoup.Jsoup.parse(engine.process("node/nodeDelete",context));
            assertThat(deletion.selectFirst("form[data-node-delete-confirm]").attr("method")).isEqualTo("post");
            assertThat(deletion.selectFirst("input[name=_tenantView]").val()).isEqualTo("current");
            context.setVariable("started",true);context.setVariable("restartInstance","old-instance");
            var restart=org.jsoup.Jsoup.parse(engine.process("restartApplication",context));
            assertThat(restart.selectFirst("[data-restart-watch]").attr("data-instance")).isEqualTo("old-instance");
            assertThat(restart.selectFirst("[data-restart-watch]").attr("data-endpoint")).isEqualTo("/application-ready");
            context.setVariable("started",false);assertThat(org.jsoup.Jsoup.parse(engine.process("restartApplication",context)).select("[data-restart-watch]")).isEmpty();
        }
    }
}
