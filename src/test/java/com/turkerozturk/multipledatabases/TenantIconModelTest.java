package com.turkerozturk.multipledatabases;

import com.turkerozturk.global.GlobalControllerAdvice;
import com.turkerozturk.sunandmoon.AstronomyWidgetService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class TenantIconModelTest {
    @Test void globalAdvicePublishesIconsForAllDatabaseNames()throws Exception {
        var holder=new CustomPropertiesHolder();holder.addCustomProperties("Test",Map.of(TenantIcons.PROPERTY,"cherry_blue"));
        var advice=new GlobalControllerAdvice();var service=mock(TenantService.class);when(service.getAllTenants()).thenReturn(Map.of("Test",mock(javax.sql.DataSource.class)));
        ReflectionTestUtils.setField(advice,"tenantService",service);ReflectionTestUtils.setField(advice,"tenantIcons",new TenantIcons(holder));ReflectionTestUtils.setField(advice,"astronomy",mock(AstronomyWidgetService.class));
        var mvc=MockMvcBuilders.standaloneSetup(new TestController()).setControllerAdvice(advice).setSingleView(new AbstractView(){protected void renderMergedOutputModel(Map<String,Object> m,jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s){}}).build();
        var result=mvc.perform(get("/icon-model")).andReturn();
        assertThat(result.getModelAndView().getModel().get("databaseIcons")).isEqualTo(Map.of("Test","cherry_blue"));
        assertThat(result.getModelAndView().getModel().get("currentDatabaseIcon")).isEqualTo("ct_db");
    }
    @Controller static class TestController{@GetMapping("/icon-model") String show(){return "page";}}
}
