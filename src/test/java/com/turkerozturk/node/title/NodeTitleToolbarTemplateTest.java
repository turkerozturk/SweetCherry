package com.turkerozturk.node.title;

import com.turkerozturk.node.Node;
import com.turkerozturk.multipledatabases.TenantForm;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeTitleToolbarTemplateTest {
    @Test void toolbarOrderEditorChoicesAndBackupMembershipRenderInBothLanguages(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            context.setVariable("_csrf",new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","csrf"));context.setVariable("tenantViewToken","current");
            var node=mock(Node.class);when(node.getNodeId()).thenReturn(9L);when(node.getName()).thenReturn("<Title> & note");when(node.getBreadcrumbs()).thenReturn(new LinkedHashMap<>());
            when(node.getNodeIcon()).thenReturn(com.turkerozturk.helpers.NodeIcon.OTHER);when(node.getSyntax()).thenReturn("custom-colors");when(node.getTxtAsHtml()).thenReturn("<div>Body</div>");
            context.setVariable("node",node);context.setVariable("requestedNodeId",9L);context.setVariable("canDeleteNode",true);context.setVariable("isSharedNode",false);context.setVariable("view","tree");context.setVariable("auditEnabled",true);context.setVariable("remindersEnabled",true);context.setVariable("nodeFragmentDepth",3);context.setVariable("nodeFragmentInclusions",20);
            var toolbar=org.jsoup.Jsoup.parse(engine.process("node/nodeToolbarFragment",context));
            assertThat(toolbar.select(".workspace-toolbar > [data-toolbar-item]").eachAttr("data-toolbar-item")).containsExactly("tags","edit","properties","move","move-to","delete","add","map","export","audit","reminder");
            assertThat(toolbar.selectFirst("a[data-toolbar-item=edit]").attr("href")).contains("/nodes/richtext/edit/9");
            assertThat(toolbar.select("[data-toolbar-item=edit] [data-duplicate]")).isEmpty();
            assertThat(toolbar.select("[data-toolbar-item=add] [data-duplicate]").eachAttr("data-duplicate")).containsExactly("single","subtree","shared");
            assertThat(toolbar.select("[data-toolbar-item=add] hr.dropdown-divider")).hasSize(2);
            context.setVariable("canEditNodeContent",true);when(node.getSyntax()).thenReturn("markdown");
            var markdown=org.jsoup.Jsoup.parse(engine.process("node/nodeToolbarFragment",context));assertThat(markdown.selectFirst("a[data-toolbar-item=edit]").attr("href")).contains("/nodes/content/9");assertThat(markdown.select("a[data-toolbar-item=edit] .fa-markdown")).hasSize(1);
            when(node.getSyntax()).thenReturn("plain-text");var plain=org.jsoup.Jsoup.parse(engine.process("node/nodeToolbarFragment",context));assertThat(plain.select("div[data-toolbar-item=edit] .dropdown-menu a")).hasSize(2);
            when(node.getSyntax()).thenReturn("custom-colors");context.setVariable("canEditNodeContent",false);
            var tree=org.jsoup.Jsoup.parse(engine.process("node/treeContentFragment",context));assertThat(tree.select("[data-title-open]")).hasSize(1);assertThat(tree.selectFirst("[data-title-form]").hasAttr("hidden")).isTrue();assertThat(tree.select("Title")).isEmpty();
            assertThat(tree.select("[data-title-form] [data-title-icon-open]")).hasSize(1);
            assertThat(tree.select("[data-title-form] input[name=icon]")).hasSize(1);
            assertThat(tree.selectFirst("[data-title-form] [data-symbol-picker-open]").text()).isEqualTo("☺");
            assertThat(tree.selectFirst("[data-title-form] [data-symbol-picker-open]").attr("aria-label")).isNotBlank();
            assertThat(tree.select("[data-title-save] .fa-save, [data-title-cancel] .fa-times")).hasSize(2);
            context.setVariable("isSharedNode",true);assertThat(org.jsoup.Jsoup.parse(engine.process("node/treeContentFragment",context)).select("[data-title-open]")).isEmpty();
            context.setVariable("isSharedNode",false);context.setVariable("canDeleteNode",false);assertThat(org.jsoup.Jsoup.parse(engine.process("node/treeContentFragment",context)).select("[data-title-open]")).isEmpty();
            var scheduled=new TenantForm();scheduled.setTenantName("Notes");scheduled.setFileName("notes.txt");var other=new TenantForm();other.setTenantName("Other");other.setFileName("other.txt");
            context.setVariable("tenants",List.of(scheduled,other));context.setVariable("databaseIcons",Map.of("Notes","zero","Other","zero"));context.setVariable("automaticBackupTenants",List.of("Notes"));context.setVariable("automaticBackupRevision","revision");
            var tenants=org.jsoup.Jsoup.parse(engine.process("tenants",context));assertThat(tenants.select("thead th")).hasSize(9);
            assertThat(tenants.select("tbody tr td:last-child span").eachText()).containsExactly(locale.getLanguage().equals("tr")?"Evet":"Yes",locale.getLanguage().equals("tr")?"Hayır":"No");
            assertThat(tenants.select("form[action='/tenants/backups/automatic/remove']")).hasSize(1);assertThat(tenants.selectFirst("input[name=revision]").val()).isEqualTo("revision");
            assertThat(tenants.outerHtml()).doesNotContain("??backupTenant.");
        }
    }
}
