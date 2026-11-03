package com.turkerozturk.customlists;
import com.turkerozturk.login.*;
import com.turkerozturk.multipledatabases.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class TemplateReportSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void reportsAreReadableByUserButClosedAndStaleSourcesNeverQuery()throws Exception {
  try(var context=new AnnotationConfigWebApplicationContext()) {
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   var service=context.getBean(com.turkerozturk.node.TemplateService.class);var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");
   var urls=java.util.List.of("/cherrytemplatenode/1","/cherrytemplate-type-1/1","/cherrytemplatenode/xlsx/1");
   for(String url:urls)mvc.perform(get(url).session(session).with(user("reader").roles("USER"))).andExpect(redirectedUrl("/"));
   TenantContext.setCurrentTenant("A");for(String url:urls)mvc.perform(get(url).session(session).param("_tenantView","old").with(user("reader").roles("USER"))).andExpect(status().isConflict());verifyNoInteractions(service);
   when(service.getTemplateNode(1,"current")).thenReturn(TemplateReportTest.report());when(service.getTemplateNode(1)).thenReturn(TemplateReportTest.report());
   for(String url:urls)mvc.perform(get(url).session(session).param("_tenantView","current").with(user("reader").roles("USER"))).andExpect(status().isOk());
   verify(service,times(2)).getTemplateNode(1,"current");verify(service).getTemplateNode(1);
  }
 }
    @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity
    static class Config implements WebMvcConfigurer {
        private static <T> org.springframework.beans.factory.FactoryBean<T> product(Class<T> type){
            T value=mock(type);return new org.springframework.beans.factory.FactoryBean<T>(){public T getObject(){return value;}public Class<?> getObjectType(){return type;}public boolean isSingleton(){return true;}};
        }
        @Bean org.springframework.web.servlet.ViewResolver viewResolver(){return (name,locale)->new org.springframework.web.servlet.view.InternalResourceView("/test-view");}
        @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
        @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
        @Bean org.springframework.beans.factory.FactoryBean<com.turkerozturk.node.TemplateService> service(){return product(com.turkerozturk.node.TemplateService.class);}
        @Bean org.springframework.beans.factory.FactoryBean<com.turkerozturk.node.NodeService> nodes(){return product(com.turkerozturk.node.NodeService.class);}
        @Bean org.springframework.context.MessageSource messageSource(){return TemplateReportTest.messages();}
        @Bean com.turkerozturk.xlsx.XlsxController xlsx(){return new com.turkerozturk.xlsx.XlsxController();}
        @Bean com.turkerozturk.dashboard.DashBoardController controller(){return new com.turkerozturk.dashboard.DashBoardController();}
        @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
        @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
            var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");
            return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));
        }
    }
}
