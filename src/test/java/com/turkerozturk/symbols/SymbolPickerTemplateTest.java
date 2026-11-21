package com.turkerozturk.symbols;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class SymbolPickerTemplateTest {
    @Test void dialogIsLocalizedUsesOnlyLocalContextAwareAssetsAndNeverSubmitsAForm(){
        for(String language:List.of("en","tr"))for(String path:List.of("","/app")){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setContextPath(path);request.setRequestURI(path+"/tree");
            var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),Locale.forLanguageTag(language));
            String html=engine.process("common/symbolPicker", java.util.Set.of("assets"), context);var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??symbols.");
            assertThat(doc.select("dialog#symbol-picker")).hasSize(1);assertThat(doc.select("form")).isEmpty();assertThat(doc.select("button").eachAttr("type")).containsOnly("button");
            assertThat(doc.select("script").eachAttr("src")).containsExactly(path+"/js/symbols/catalog.js",path+"/js/symbols/inlineSymbols.js",path+"/js/symbols/images.js",path+"/js/symbols/picker.js");
            assertThat(doc.select("script[data-ctb-base]").attr("data-ctb-base")).isEqualTo(path+"/img/ctbicons/");
            assertThat(doc.select("[data-symbol-kind] option").eachAttr("value")).containsExactly("all","unicode","ctb","solid","regular","brands");assertThat(doc.selectFirst("[data-symbol-value]").hasAttr("readonly")).isTrue();
        }
    }
}
