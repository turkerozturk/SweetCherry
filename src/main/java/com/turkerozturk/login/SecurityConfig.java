/*
 * This file is part of the SweetCherry project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/SweetCherry
 *
 * Copyright (c) 2024 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.login;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // value after semicolon is default value
    // https://www.baeldung.com/spring-value-defaults
    @Value("${myapp.login.user.name:user}")
    String loginUserName;

    @Value("${myapp.login.user.password:}")
    String loginUserPassword;

    @Value("${myapp.login.admin.name:admin}")
    String loginAdminName;

    @Value("${myapp.login.admin.password:}")
    String loginAdminPassword;

    @Bean
    InMemoryUserDetailsManager users() throws IOException {
        Properties local = LocalLoginCredentials.load(Path.of("login-credentials.properties"));
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withUsername(loginUserName)
                        .password("{bcrypt}" + encoder.encode(loginUserPassword.isBlank()
                                ? local.getProperty("user.password") : loginUserPassword))
                        .roles("USER")
                        .build(),
                User.withUsername(loginAdminName)
                        .password("{bcrypt}" + encoder.encode(loginAdminPassword.isBlank()
                                ? local.getProperty("admin.password") : loginAdminPassword))
                        .roles("ADMIN")
                        .build()
        );
    }


    // https://www.youtube.com/watch?v=aMd3P_5bB6s&list=PLmxVbmyIiPPs2s_06jw6ELX4HYyN1Vv_b&index=3
    //  Spring Security 6: Personalize Your Login Experience
    // https://www.baeldung.com/spring-deprecated-websecurityconfigureradapter
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/nodes/delete/**", "/upload-form", "/upload-database", "/reload-datasources", "/delete/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/img/**", "/about", "/main", "/features", "/static/css/**", "/webjars/**",
                                "/sw.js", "/workbox-*.js",
                                "/actuator/health", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                )
                .logout(a -> a
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(logoutSuccessHandler())
                        .permitAll()
                )
                .build();
    }

    /**
     * Bu satır, çıkış işlemi başarılı olduğunda ne yapılacağını belirten bir LogoutSuccessHandler
     * tanımlar. Bu örnekte, logoutSuccessHandler() metodundan dönen handler kullanılır.
     * Handler, çıkış işlemi tamamlandıktan sonra kullanıcıyı belirli bir URL'ye yönlendirir.
     * Aşağıdaki metod kaldırılırsa logout olur olmaz login sayfası gelecekti.
     * Bu sayede logout sonrası bir mesaj göstermek veya logout işleminin başarılı olduğunu
     * belirtmek için ek bir sayfa gösterilebilir. Bu yapılandırma, logout işleminden sonra
     * kullanıcıyı doğrudan login sayfasına yönlendirmek yerine,
     * önce logout sayfasını göstererek login sayfasına gitmeyi engeller.
     * @return
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/login?logout");
        };
    }


}

       /* http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry.requestMatchers(HttpMethod.DELETE).hasRole("ADMIN")
                                .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                                .requestMatchers("/login/**").permitAll()
                                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(httpSecuritySessionManagementConfigurer -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
*/
