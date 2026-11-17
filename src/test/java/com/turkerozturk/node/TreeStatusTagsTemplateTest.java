package com.turkerozturk.node;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class TreeStatusTagsTemplateTest {
    @Test void footerShowsEscapedTagsLastForRealAndSharedNodesInBothLanguages() {
        for (var locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("tr"))) {
            for (boolean shared : List.of(false, true)) {
                var node = new Node(); node.setNodeId(10); node.setSyntax("plain-text");
                node.setTags("todo önemli <script>alert(1)</script>");
                String html = render(node, shared, locale);
                var doc = org.jsoup.Jsoup.parse(html);
                var tags = doc.selectFirst(".workspace-status-tags");
                assertThat(tags).isNotNull();
                assertThat(tags.text()).contains(node.getTags(), locale.getLanguage().equals("tr") ? "Etiketler" : "Tags");
                assertThat(tags.attr("title")).isEqualTo(node.getTags());
                assertThat(tags.nextElementSibling()).isNull();
                assertThat(doc.select("script")).isEmpty();
                assertThat(html).doesNotContain("??");
            }
        }
    }
    @Test void footerOmitsTagsForEmptyUntaggedAndDatabaseRootViews() {
        for (String tags : new String[]{null, "", "   "}) {
            var node = new Node(); node.setNodeId(10); node.setSyntax("plain-text"); node.setTags(tags);
            assertThat(org.jsoup.Jsoup.parse(render(node, false, Locale.ENGLISH)).select(".workspace-status-tags")).isEmpty();
        }
        assertThat(org.jsoup.Jsoup.parse(render(null, false, Locale.ENGLISH)).select(".workspace-status-tags")).isEmpty();
    }
    private String render(Node node, boolean shared, Locale locale) {
        var engine = new SpringTemplateEngine();
        var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8"); engine.setTemplateResolver(resolver);
        var messages = new ResourceBundleMessageSource(); messages.setBasename("messages"); messages.setDefaultEncoding("UTF-8"); engine.setTemplateEngineMessageSource(messages);
        var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
        var context = new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request, new MockHttpServletResponse()), locale);
        context.setVariable("node", node); context.setVariable("isSharedNode", shared); context.setVariable("requestedNodeId", 55L);
        context.setVariable("currentTenantName", "Demo"); context.setVariable("currentDatabaseIcon", "ct_home");
        return engine.process("node/treeContentFragment", Set.of("workspaceStatus"), context);
    }
}
