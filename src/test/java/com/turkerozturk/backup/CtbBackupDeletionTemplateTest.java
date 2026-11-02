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

class CtbBackupDeletionTemplateTest {
    @Test void confirmationShowsExactFilesReasonsTotalsAndASeparatePostTokenInBothLanguages()throws Exception {
        for(String lang:List.of("en","tr"))for(boolean eligible:List.of(true,false)) {
            var locale=Locale.forLanguageTag(lang);var engine=new SpringTemplateEngine();engine.setTemplateResolver(new StringTemplateResolver());
            var messages=new StaticMessageSource();var properties=new Properties();
            try(var in=Files.newBufferedReader(Path.of("src/main/resources/messages_"+lang+".properties"))){properties.load(in);}
            properties.forEach((k,v)->messages.addMessage(k.toString(),locale,v.toString()));engine.setTemplateEngineMessageSource(messages);
            var servlet=new MockServletContext();var context=new WebContext(JakartaServletWebApplication.buildApplication(servlet).buildExchange(new MockHttpServletRequest(servlet),new MockHttpServletResponse()),locale);
            String id="20261102-000000-"+UUID.randomUUID();
            var backup=new CtbBackupStore.Backup(id,"backup-"+id+".ctb","<script>name</script>","2026-11-02T00:00:00Z",1024,"a".repeat(64),Map.of());
            var item=new CtbBackupDeletionService.Item(id,backup,eligible?null:"ctbBackup.error.protected");
            context.setVariable("deletion",new CtbBackupDeletionService.Preview("confirmation-token",List.of(item),eligible?1:0,eligible?1024:0));
            String html=engine.process(Files.readString(Path.of("src/main/resources/templates/backup/ctbBackupDelete.html")),context);var doc=org.jsoup.Jsoup.parse(html);
            assertThat(html).doesNotContain("??ctbBackup", "<script>name</script>");assertThat(doc.selectFirst("input[name=token]").val()).isEqualTo("confirmation-token");
            assertThat(doc.select("input[name=ids]")).isEmpty();assertThat(doc.selectFirst("form").attr("action")).isEqualTo("/system/backups/delete/confirm");
            assertThat(doc.selectFirst("button[type=submit]").hasAttr("disabled")).isEqualTo(!eligible);assertThat(doc.selectFirst("a").attr("href")).isEqualTo("/system/backups");
        }
    }
}
