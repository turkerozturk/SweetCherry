package com.turkerozturk.node.properties;

import com.turkerozturk.helpers.NodeIcon;
import com.turkerozturk.node.Node;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NodeSearchFlagsTest {
    @Test void checkboxSubmissionBindsCheckedAndUncheckedValues() throws Exception {
        var service=mock(NodePropertiesService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new NodePropertiesController(service)).build();
        for(int flags=0;flags<4;flags++) {
            mvc.perform(post("/nodes/properties/12").param("name","Name").param("color","NONE")
                    .param("icon","OTHER").param("excludeThisNode",(flags&1)!=0?new String[]{"true","false"}:new String[]{"false"})
                    .param("excludeSubnodes",(flags&2)!=0?new String[]{"true","false"}:new String[]{"false"}))
                    .andExpect(status().is3xxRedirection());
            verify(service).update(12,"Name",false,TitleColor.NONE,NodeIcon.OTHER,false,null,null,null,(flags&1)!=0,(flags&2)!=0);
        }
    }
    @Test void propertiesAndTitleStatusesRenderInBothLanguages() {
        var service=mock(NodePropertiesService.class);when(service.writable()).thenReturn(true);
        var node=new Node();node.setNodeId(12);node.setName("Test");node.setSyntax("plain-text");
        when(service.realNode(12)).thenReturn(node);
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
            for(int flags=0;flags<4;flags++) {
                node.setLevel(flags);node.setIsReadOnly16bit(1);node.parseNodeIconAndReadOnlyFlag();
                var model=new org.springframework.ui.ExtendedModelMap();new NodePropertiesController(service).edit(12,model);
                var html=render("node/nodeProperties",model.asMap(),locale);
                var doc=org.jsoup.Jsoup.parse(html);
                assertThat(doc.selectFirst("input[type=checkbox][name=excludeThisNode]").hasAttr("checked")).isEqualTo((flags&1)!=0);
                assertThat(doc.selectFirst("input[type=checkbox][name=excludeSubnodes]").hasAttr("checked")).isEqualTo((flags&2)!=0);
                assertThat(html).doesNotContain("??node.");
                assertThat(doc.text()).contains(locale.getLanguage().equals("tr")?"Aramalardan Hariç Tut":"Exclude from Searches");
                var header=org.jsoup.Jsoup.parse(render("node/nodeMetaDataFragment",Map.of("node",node,"nodeBookmarked",true),locale));
                var icons=header.select(".node-title-states img");
                assertThat(icons.eachAttr("src")).containsExactlyElementsOf(flags==0?
                        List.of("/img/ctbicons/ct_locked.png","/img/ctbicons/ct_pin.png"):
                        List.of("/img/ctbicons/ct_locked.png","/img/ctbicons/ct_pin.png","/img/ctbicons/ct_ghost.png"));
                assertThat(icons.eachAttr("title")).allMatch(value->!value.isBlank()&&!value.contains("??"));
            }
        }
    }
    @Test void treeKeepsAllStatusCombinationsInOneSlotAfterOwnIcon() {
        for(int mask=0;mask<8;mask++) {
            var item=new com.turkerozturk.children.NaviNode(12,"Test",NodeIcon.CT_HOME,false,null,
                    (mask&1)!=0,false,1,null,null,(mask&2)!=0,(mask&4)!=0);
            var html=render("node/readerContextNavigation",Map.of("siblingNaviNodes",List.of(item),
                    "requestedNodeId",12L,"childNodes",List.of(),"currentDatabaseIcon","ct_notes","currentTenantName","Test"),Locale.ENGLISH);
            var link=org.jsoup.Jsoup.parse(html).selectFirst(".reader-context-list a");
            assertThat(link.child(0).tagName()).isEqualTo("img");
            assertThat(link.select(".node-state-stack")).hasSize(mask==0?0:1);
            if(mask!=0) {
                var stack=link.child(1);assertThat(stack.className()).isEqualTo("node-state-stack");
                assertThat(stack.attr("data-count")).isEqualTo(Integer.toString(Integer.bitCount(mask)));
                var icons=new ArrayList<String>();if((mask&1)!=0)icons.add("/img/ctbicons/ct_locked.png");
                if((mask&2)!=0)icons.add("/img/ctbicons/ct_ghost.png");if((mask&4)!=0)icons.add("/img/ctbicons/ct_pin.png");
                assertThat(stack.select("img").eachAttr("src")).containsExactlyElementsOf(icons);
            }
            assertThat(link.children().last().text()).isEqualTo("Test");
        }
    }
    private String render(String template,Map<String,Object> model,Locale locale) {
        var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setTemplateMode("HTML");engine.setTemplateResolver(resolver);
        var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var response=new MockHttpServletResponse();
        var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,response),locale);
        context.setVariables(model);return engine.process(template,context);
    }
}
