package com.turkerozturk.image.attachments.excel;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class ExcelPreviewTemplateTest {
    @Test void bothLanguagesEscapeAllFileContentAndPreserveSheetAndTenantAcrossPages() throws Exception {
        for (var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) {
            var engine=new SpringTemplateEngine();var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");engine.setTemplateResolver(resolver);
            var messages=new ResourceBundleMessageSource();messages.setBasename("messages");messages.setDefaultEncoding("UTF-8");engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            var cell=new ExcelPreviewService.CellValue("<img src=x onerror=alert(1)>","=HYPERLINK(\"javascript:alert(1)\")",true,false,"left");
            context.setVariable("excel",new ExcelPreviewService.Preview("<file>.xlsx","<node>",List.of(new ExcelPreviewService.SheetInfo(0,"<sheet>",true)),0,1,2,100,List.of("A"),List.of(new ExcelPreviewService.RowValue(1,List.of(cell))),true,false));
            context.setVariable("nodeId",1L);context.setVariable("offset",3);context.setVariable("tenantViewToken","current");
            String html=engine.process("attachment/excelContent",context);
            assertThat(html).doesNotContain("??","<img src=x","<file>","<sheet>").contains("&lt;img","&lt;file&gt;.xlsx","sheet=0","page=2","_tenantView=current","/attachments/1/3/download","<details");
            var dom=org.jsoup.Jsoup.parse(html);assertThat(dom.select("input:not([type=hidden]),textarea,[contenteditable]")).isEmpty();assertThat(dom.select("a[href^=javascript]")).isEmpty();
            context.removeVariable("excel");context.setVariable("excelError","encrypted");assertThat(engine.process("attachment/excelContent",context)).doesNotContain("??").contains("role=\"alert\"");
        }
    }
}
