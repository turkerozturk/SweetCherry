package com.turkerozturk.settings;

import com.turkerozturk.settings.editing.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class AppearanceSettingsTest {
    Map<String,String> defaults(){var values=new LinkedHashMap<String,String>();ApplicationSettingsService.SETTINGS.forEach(s->values.put(s.key(),s.fallback()));return values;}
    @Test void themeUsesLightUnlessDarkIsExplicitlyConfigured(){
        assertThat(new UiThemeAdvice("light").theme()).isEqualTo("light");
        assertThat(new UiThemeAdvice(" DARK ").theme()).isEqualTo("dark");
        assertThat(new UiThemeAdvice("system").theme()).isEqualTo("light");
        assertThat(new UiThemeAdvice("<script>").theme()).isEqualTo("light");
    }
    @Test void additionalSettingsRejectInvalidBoundsAndEnumsButAcceptZeroAndSystemDefaults(){
        var good=defaults();good.put("myapp.home.created-minimum","0");good.put("myapp.appearance.theme","dark");
        assertThat(ApplicationSettingsService.update("myapp: {}\n",good)).contains("myapp.home.created-minimum: 0","myapp.appearance.theme: 'dark'");
        var bad=Map.of("myapp.appearance.theme","auto<script>","myapp.default-language","fr","myapp.tree.default-width","19","myapp.home.created-minimum","-1","myapp.calendar.day-max-entries","2001","myapp.calendar.day-preview-max-lines","0","myapp.calendar.time-zone","bad/zone","myapp.calendar.first-day-of-week","8","myapp.custom-lists.views.date-time-format","invalid[","myapp.custom-lists.views.max-design-characters","1048577");
        bad.forEach((key,value)->{var values=defaults();values.put(key,value);assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",values)).as(key).isInstanceOf(RuntimeException.class);});
    }
    @Test void treeDensityDefaultsAndBoundsAreSafe(){
        var normal=new TreeAppearanceAdvice("16","1");assertThat(normal.fontSize()).isEqualTo(16);assertThat(normal.verticalPadding()).isEqualTo(1);
        var invalid=new TreeAppearanceAdvice("1;display:none","-2");assertThat(invalid.fontSize()).isEqualTo(16);assertThat(invalid.verticalPadding()).isEqualTo(1);
        assertThat(new TreeAppearanceAdvice("32","0").verticalPadding()).isZero();
        for(var entry:Map.of("myapp.tree.font-size","9","myapp.tree.vertical-padding","13").entrySet()){var values=defaults();values.put(entry.getKey(),entry.getValue());assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",values)).isInstanceOf(IllegalArgumentException.class);}
        var values=defaults();values.put("myapp.tree.vertical-padding","0");values.put("myapp.tree.font-size","10");assertThat(ApplicationSettingsService.update("myapp: {}\n",values)).contains("myapp.tree.vertical-padding: 0","myapp.tree.font-size: 10");
    }
    @Test void settingsRenderAllFieldsWithLocalizedChoicesInBothLanguages(){
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))){
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setTemplateMode("HTML");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
            var values=defaults();values.put("myapp.calendar.first-day-of-week","7");
            context.setVariable("settings",ApplicationSettingsService.SETTINGS);context.setVariable("snapshot",new ApplicationSettingsService.Snapshot("application.yml","revision",values,values));
            var html=engine.process("applicationSettings",context);var doc=org.jsoup.Jsoup.parse(html);
            assertThat(html).doesNotContain("??appsettings.");
            assertThat(doc.select("select[name=myapp.appearance.theme] option").eachAttr("value")).containsExactly("light","dark");
            assertThat(doc.selectFirst("select[name=myapp.calendar.first-day-of-week] option[selected]").attr("value")).isEqualTo("SUNDAY");
            assertThat(doc.selectFirst("input[name=myapp.home.created-minimum]").attr("min")).isEqualTo("0");
            assertThat(doc.selectFirst("input[name=myapp.custom-lists.views.time-zone]").hasAttr("required")).isFalse();
            for(var setting:ApplicationSettingsService.SETTINGS)assertThat(doc.getElementsByAttributeValue("name",setting.key())).hasSize(1);
        }
    }
}
