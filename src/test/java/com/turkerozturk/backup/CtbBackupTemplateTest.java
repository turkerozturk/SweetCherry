package com.turkerozturk.backup;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import static org.assertj.core.api.Assertions.*;

class CtbBackupTemplateTest {
    @Test void completedAndRunningViewsAreTranslatedEscapedAndDoNotRequireAnActiveTenant() throws Exception {
        for(String lang:List.of("tr","en"))for(boolean running:List.of(false,true)) {
            var locale=Locale.forLanguageTag(lang);var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());
            var messages=new StaticMessageSource();var properties=new Properties();
            try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+lang+".properties"))){properties.load(in);}
            properties.forEach((k,v)->messages.addMessage(k.toString(),locale,v.toString()));engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);
            var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse()),locale);
            String id="20261102-120000-"+UUID.randomUUID();
            var backup=new CtbBackupStore.Backup(id,"backup-"+id+".ctb","<Source>","2026-11-02T12:00:00Z",1024,"a".repeat(64),Map.of("node",4L));
            context.setVariable("backupEnabled",true);context.setVariable("backupSelected",running?"<Source>":null);
            context.setVariable("backupDirectory","backups/ctb");context.setVariable("backupInvalid",0);context.setVariable("backups",List.of(backup));
            context.setVariable("tenantViewToken","token");
            context.setVariable("backupAutomatic",new CtbBackupScheduler.View(running,1440,running?2:0,"—",null,
                    running?List.of(new CtbBackupScheduler.Row(List.of("<Source>","Alias"),"DUE","—","—",null)):List.of()));
            if(running)context.setVariable("backupCurrent",new CtbBackupService.Status(id,"<Source>","2026-11-02T12:00:00Z","RUNNING","COPYING",5,10,null));
            String html=engine.process(Files.readString(Path.of("src/main/resources/templates/backup/ctbBackups.html")),context);
            var doc=org.jsoup.Jsoup.parse(html);assertThat(html).doesNotContain("??ctbBackup", "<Source>");
            assertThat(doc.selectFirst("a[href$=/download]").attr("href")).endsWith(id+"/download");
            assertThat(doc.selectFirst("form[action$=/open]").attr("method")).isEqualTo("post");
            assertThat(doc.select("form[action$=/start]")).hasSize(running?1:0);
            if(running)assertThat(doc.selectFirst("input[name=_tenantView]").attr("value")).isEqualTo("token");
            assertThat(doc.select(".backup-list td[data-label]")).hasSize(running?8:4);
            assertThat(doc.selectFirst("#ctb-backup-automatic")).isNotNull();
            assertThat(doc.selectFirst("h1").text()).isEqualTo(lang.equals("tr")?"CTB yedekleri":"CTB backups");
        }
    }
}
