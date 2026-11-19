package com.turkerozturk.node.fragments;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import com.turkerozturk.node.Node;
import static org.assertj.core.api.Assertions.*;

class NodeFragmentTemplateTest {
    @Test void hostMetadataAndRememberedViewControlRenderInBothLanguages(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var node=new Node();node.setNodeId(2L);node.setTxt("text");node.setTxtAsHtml("<p>content</p>");node.setSyntax("custom-colors");
            context.setVariable("node",node);context.setVariable("tenantViewToken","current");context.setVariable("nodeFragmentDepth",3);context.setVariable("nodeFragmentInclusions",20);
            String html=engine.process("node/nodeContentFragment",context);var root=org.jsoup.Jsoup.parse(html).selectFirst("[data-node-fragments]");
            assertThat(root.attr("data-fragment-content")).isEqualTo("2");assertThat(root.attr("data-fragment-token")).isEqualTo("current");assertThat(root.attr("data-fragment-depth")).isEqualTo("3");assertThat(html).doesNotContain("??");
            String menu=engine.process("common/viewModeFragment",context);assertThat(menu).doesNotContain("??");assertThat(org.jsoup.Jsoup.parse(menu).selectFirst("[data-node-fragments-source-toggle]").attr("aria-pressed")).isEqualTo("false");
            String footer=engine.process("common/footer",context);assertThat(footer).contains("/js/common/nodeFragmentFlow.js");assertThat(footer.indexOf("/js/common/nodeFragmentFlow.js")).isLessThan(footer.indexOf("/js/common/nodeFragments.js"));assertThat(org.jsoup.Jsoup.parse(menu).selectFirst("[data-node-fragments-toggle]").attr("aria-pressed")).isEqualTo("false");
        }
    }
}
