package com.turkerozturk.image.attachments.drawio;

import com.turkerozturk.settings.editing.ApplicationSettingsService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DrawioSettingsTest {
    static DrawioSettings with(String component,int value)throws Exception{
        var parts=DrawioSettings.class.getRecordComponents();var args=new Object[parts.length];var types=new Class<?>[parts.length];
        for(int n=0;n<parts.length;n++){types[n]=int.class;args[n]=parts[n].getName().equals(component)?value:parts[n].getAccessor().invoke(DrawioSettings.defaults());}
        return DrawioSettings.class.getDeclaredConstructor(types).newInstance(args);
    }
    Map<String,String> defaults(){var map=new LinkedHashMap<String,String>();ApplicationSettingsService.SETTINGS.forEach(s->map.put(s.key(),s.fallback()));return map;}
    @Test void allResourceBudgetsHaveMatchingYamlDefaultsAndEditableValidatedSettings()throws Exception{
        var parts=DrawioSettings.class.getRecordComponents();var constructor=DrawioSettings.class.getDeclaredConstructors()[0];
        for(int n=0;n<parts.length;n++){
            String property=constructor.getParameters()[n].getAnnotation(org.springframework.beans.factory.annotation.Value.class).value();String key=property.substring(2,property.indexOf(':'));
            var entry=ApplicationSettingsService.SETTINGS.stream().filter(s->s.key().equals(key)).findFirst().orElseThrow();
            assertThat(parts[n].getAccessor().invoke(DrawioSettings.defaults()).toString()).isEqualTo(entry.fallback());
            var values=defaults();values.put(key,Long.toString(entry.maximum()));assertThat(ApplicationSettingsService.update("myapp: {}\n",values)).contains(key+": "+entry.maximum());
            values.put(key,Long.toString(entry.maximum()+1));assertThatThrownBy(()->ApplicationSettingsService.update("myapp: {}\n",values)).isInstanceOf(IllegalArgumentException.class);
            String component=parts[n].getName();int invalid=(int)entry.maximum()+1;assertThatThrownBy(()->with(component,invalid)).hasCauseInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void configuredLimitsReportTheExactYamlSettingInsteadOfAnUnexplainedFailure()throws Exception{
        String model=DrawioServiceTest.model("a".repeat(1000));
        for(var item:Map.of("pageBytes","max-page-bytes","cells","max-cells-per-page","totalCells","max-total-cells","totalBytes","max-total-bytes","labelCharacters","max-label-characters").entrySet()){
            var limits=with(item.getKey(),item.getKey().equals("pageBytes")||item.getKey().equals("totalBytes")?500:2);
            assertThatThrownBy(()->DrawioDocument.parse(DrawioServiceTest.bytes(model),"test.drawio","Node",limits)).isInstanceOf(DrawioService.Problem.class).satisfies(e->assertThat(((DrawioService.Problem)e).limit()).isEqualTo(item.getValue()));
        }
    }
    @Test void uriEncodedExpansionIsNotMistakenForDecodedPageSize()throws Exception{
        String text="界".repeat(20000),model=DrawioServiceTest.model(text);
        var limits=with("pageBytes",100000);
        var document=DrawioDocument.parse(DrawioServiceTest.bytes("<mxfile><diagram>"+DrawioServiceTest.compressed(model)+"</diagram></mxfile>"),"test.drawio","Node",limits);
        assertThat(document.pages()).hasSize(1);assertThat(document.maxPageBytes()).isEqualTo(100000);assertThat(document.pages().get(0).xml()).contains(text);
    }
    @Test void expandedPagesAboveThePreviousTwoMiBLimitAreAcceptedByNewDefaults()throws Exception{
        var model=new StringBuilder("<mxGraphModel><root><mxCell id='0'/><mxCell id='1' parent='0'/>");
        for(int n=2;n<302;n++)model.append("<mxCell id='").append(n).append("' parent='1' vertex='1' value='").append("text1234".repeat(1000)).append("'/>");
        model.append("</root></mxGraphModel>");assertThat(model.length()).isGreaterThan(2*1024*1024);
        var document=DrawioDocument.parse(DrawioServiceTest.bytes("<mxfile><diagram>"+DrawioServiceTest.compressed(model.toString())+"</diagram></mxfile>"),"large.drawio","Node");
        assertThat(document.pages()).hasSize(1);
    }
    @Test void safeHtmlAlignmentBreaksAndStylesSurviveWhileLinksAndActiveCssAreRemoved(){
        String value="&lt;div style=&quot;text-align:right;white-space:pre-wrap;background-image:url(https://evil)&quot;&gt;one&lt;br&gt;&lt;b&gt;two&lt;/b&gt;&lt;/div&gt;&lt;a href=&quot;javascript:evil()&quot; onclick=&quot;evil()&quot;&gt;link text&lt;/a&gt;";
        String source=DrawioServiceTest.model(value).replace("rounded=1;","align=left;verticalAlign=top;rounded=1;");
        String xml=DrawioDocument.parse(DrawioServiceTest.bytes(source),"test.drawio","Node").pages().get(0).xml();
        var cell=org.jsoup.Jsoup.parse(xml,"",org.jsoup.parser.Parser.xmlParser()).selectFirst("mxCell[vertex=1]");
        assertThat(cell.attr("style")).contains("align=left","verticalAlign=top","whiteSpace=wrap","html=1");
        var html=org.jsoup.Jsoup.parse(cell.attr("value"));assertThat(html.selectFirst("div").attr("style")).contains("text-align:right","white-space:pre-wrap");assertThat(html.select("br")).hasSize(1);assertThat(html.selectFirst("b").text()).isEqualTo("two");assertThat(html.text()).contains("link text");
        assertThat(cell.attr("value")).doesNotContain("href","onclick","javascript:","url(","https://");
        String plain=DrawioServiceTest.model("one&#xa;two").replace("html=1","html=0");var result=DrawioDocument.parse(DrawioServiceTest.bytes(plain),"test.drawio","Node").pages().get(0).xml();
        assertThat(org.jsoup.Jsoup.parse(result,"",org.jsoup.parser.Parser.xmlParser()).selectFirst("mxCell[vertex=1]").attr("value")).contains("one<br>two");
    }
    @Test void drawioSettingsRenderAsLocalizedEditableFieldsInBothLanguages(){
        for(String language:List.of("en","tr")){
            var engine=new org.thymeleaf.spring6.SpringTemplateEngine();var resolver=new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");engine.setTemplateResolver(resolver);
            var messages=new org.springframework.context.support.ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new org.springframework.mock.web.MockServletContext();var request=new org.springframework.mock.web.MockHttpServletRequest(servlet);
            var context=new org.thymeleaf.context.WebContext(org.thymeleaf.web.servlet.JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new org.springframework.mock.web.MockHttpServletResponse()),Locale.forLanguageTag(language));
            var values=defaults();context.setVariable("settings",ApplicationSettingsService.SETTINGS);context.setVariable("snapshot",new ApplicationSettingsService.Snapshot("application.yml","revision",values,values));
            String html=engine.process("applicationSettings",context);assertThat(html).doesNotContain("??appsettings.");var doc=org.jsoup.Jsoup.parse(html);
            for(var setting:ApplicationSettingsService.SETTINGS)if(setting.key().startsWith("myapp.drawio-preview.")){
                var input=doc.getElementsByAttributeValue("name",setting.key());assertThat(input).hasSize(1);
                if(setting.type().equals("boolean")){assertThat(input.first().tagName()).isEqualTo("select");assertThat(input.select("option[selected]").attr("value")).isEqualTo("true");}
                else if(!setting.type().equals("localdir")){assertThat(input.attr("value")).isEqualTo(setting.fallback());assertThat(input.attr("max")).isEqualTo(Long.toString(setting.maximum()));}
            }
        }
    }

}
