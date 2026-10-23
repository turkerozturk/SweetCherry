package com.turkerozturk.language;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class LocaleConfigurationTest {
    private MockHttpServletRequest request(String... languages) {
        var request=new MockHttpServletRequest();request.setPreferredLocales(Arrays.stream(languages).map(Locale::forLanguageTag).toList());return request;
    }
    @Test void browserLanguageIsUsedWithoutAConfiguredDefault() {
        assertThat(LocaleConfiguration.initialLocale(request("en-US","tr"),"").getLanguage()).isEqualTo("en");
        assertThat(LocaleConfiguration.initialLocale(request("tr-TR","en"),"").getLanguage()).isEqualTo("tr");
        assertThat(LocaleConfiguration.initialLocale(request("de-DE","tr"),"").getLanguage()).isEqualTo("tr");
        assertThat(LocaleConfiguration.initialLocale(request("de-DE"),"").getLanguage()).isEqualTo("en");
    }
    @Test void YAMLDefaultWinsOverBrowserButHistoricalListDoesNot() {
        assertThat(LocaleConfiguration.initialLocale(request("en"),"tr_TR").getLanguage()).isEqualTo("tr");
        assertThat(LocaleConfiguration.initialLocale(request("tr"),"en-US").getLanguage()).isEqualTo("en");
        assertThat(LocaleConfiguration.initialLocale(request("tr"),"en_US, tr_TR").getLanguage()).isEqualTo("tr");
    }
    @Test void chosenLocaleCookieOverridesDefaultAndMenuCanChangeIt()throws Exception {
        var config=new LocaleConfiguration();ReflectionTestUtils.setField(config,"configuredLanguage","en");var resolver=config.localeResolver();
        var request=request("en");request.setCookies(new jakarta.servlet.http.Cookie("locale","tr"));assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("tr");
        request=request("en");request.setAttribute(org.springframework.web.servlet.DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,resolver);request.addParameter("lang","tr");
        var interceptor=new org.springframework.web.servlet.i18n.LocaleChangeInterceptor();interceptor.setParamName("lang");var response=new MockHttpServletResponse();interceptor.preHandle(request,response,new Object());
        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("tr");assertThat(response.getCookie("locale")).isNotNull();
    }
}
