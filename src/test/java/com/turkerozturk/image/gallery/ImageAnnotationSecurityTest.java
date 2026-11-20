package com.turkerozturk.image.gallery;

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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImageAnnotationSecurityTest {
 @AfterEach void clear(){TenantContext.clear();}
 @Test void pngReplacementRequiresAdminCsrfConfirmationAndTheCurrentTenant()throws Exception{
  try(var context=new AnnotationConfigWebApplicationContext()){
   context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
   String route="/image-gallery/1/7/annotation";var session=new MockHttpSession();session.setAttribute(TenantContext.SESSION_VARIABLE__TENANT_VIEW_TOKEN,"current");TenantContext.setCurrentTenant("Notes");byte[] png=ImageAnnotationServiceTest.picture(0,20,10);
   for(String method:java.util.List.of("GET","POST")){
    var request=method.equals("GET")?get(route):post(route).contentType("image/png").content(png);
    mvc.perform(request.session(session).param("revision","r").param("confirmed","true").param("_tenantView","current").with(user("reader").roles("USER")).with(csrf())).andExpect(status().isForbidden());
   }
   mvc.perform(post(route).session(session).contentType("image/png").content(png).param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
   mvc.perform(post(route).session(session).contentType("image/png").content(png).param("revision","r").param("confirmed","true").param("_tenantView","old").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   mvc.perform(post(route).session(session).contentType("image/png").content(png).param("revision","r").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
   mvc.perform(post(route).session(session).contentType("image/png").content(png).param("revision","r").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
   ImageAnnotationService service=org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(ImageAnnotationService.class));verifyNoInteractions(service);
   mvc.perform(post(route).session(session).contentType("image/png").content(png).param("revision","r").param("confirmed","true").param("_tenantView","current").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());verify(service).save(eq(1L),eq(7),eq("r"),org.mockito.AdditionalMatchers.aryEq(png),eq(true));
  }
 }
 @Configuration @EnableWebSecurity @EnableWebMvc @EnableMethodSecurity static class Config implements WebMvcConfigurer{
  @Bean jakarta.persistence.EntityManagerFactory entityManagerFactory(){return mock(jakarta.persistence.EntityManagerFactory.class);}
  @Bean InMemoryUserDetailsManager users(){return new InMemoryUserDetailsManager();}
  @Bean ImageAnnotationService service(){return mock(ImageAnnotationService.class);}
  @Bean ImageAnnotationController controller(ImageAnnotationService service){return new ImageAnnotationController(service);}
  @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new TenantRequiredInterceptor());}
  @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{var config=new SecurityConfig();ReflectionTestUtils.setField(config,"loginUserName","user");ReflectionTestUtils.setField(config,"loginAdminName","admin");return config.filterChain(http,new LoginAttemptLimiter(),new LoginClientAddressResolver(""));}
 }
}
