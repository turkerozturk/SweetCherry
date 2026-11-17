package com.turkerozturk.node.todo;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class TodoTemplateTest {
    @Test void translationsSafeNodeLinksAndWriteControlsRenderForBothRolesAndLanguages() {
        for (var locale : List.of(Locale.ENGLISH,Locale.forLanguageTag("tr"))) for (boolean writable : List.of(true,false)) {
            var task = new TodoService.Task(11,1,"<Task>","ct_home",true,"#3584E4",List.of("todo","<script>"),false,2,"Parent / <Task>","revision");
            var view = new TodoService.View(List.of(task),1,0,1,new TodoService.Options("","open",50,1));
            String html = render(view,writable,locale,"node/todo"); var doc = org.jsoup.Jsoup.parse(html);
            assertThat(html).contains("&lt;Task&gt;","&lt;script&gt;","_tenantView=token").doesNotContain("??");
            assertThat(doc.select("script")).isEmpty();
            assertThat(doc.select("a[href*=/nodes/11]")).hasSize(1);
            assertThat(doc.select("form[method=post]")).hasSize(writable?1:0);
            if(writable) { assertThat(doc.selectFirst("form[method=post]").attr("action")).isEqualTo("/nodes/todo/11/state"); assertThat(doc.selectFirst("input[name=revision]").val()).isEqualTo("revision"); }
        }
    }
    @Test void emptyOutsidePageAndConflictHaveUsefulNavigation() {
        var empty = new TodoService.View(List.of(),0,0,0,new TodoService.Options("","open",50,3));
        assertThat(render(empty,false,Locale.ENGLISH,"node/todo")).contains("No tasks match", "Return to first page");
        assertThat(render(empty,false,Locale.ENGLISH,"node/todo-error")).contains("Refresh the task list", "Return to tasks");
    }
    private String render(TodoService.View view,boolean writable,Locale locale,String template) {
        var engine = new SpringTemplateEngine(); var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8"); engine.setTemplateResolver(resolver);
        var messages = new ResourceBundleMessageSource(); messages.setBasename("messages"); messages.setDefaultEncoding("UTF-8"); engine.setTemplateEngineMessageSource(messages);
        var servlet = new MockServletContext(); var request = new MockHttpServletRequest(servlet);
        var context = new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
        context.setVariable("tasks",view); context.setVariable("canEditTasks",writable); context.setVariable("tenantViewToken","token"); context.setVariable("todoError","changed");
        return engine.process(template,context);
    }
}
