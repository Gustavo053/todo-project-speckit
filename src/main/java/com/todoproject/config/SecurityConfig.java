package com.todoproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security is present for its protections only — there is no authentication at this stage
 * (research R6). Every route is open; "no login" removes logins, not defences.
 *
 * <ul>
 *   <li><b>CSRF enabled</b> for the HTML form routes. Thymeleaf injects the token into every form
 *       automatically, so the non-scripted flows keep working unchanged.
 *   <li><b>CSRF disabled for {@code /api/**}</b> only. That surface is stateless, cookie-less and
 *       carries no ambient credential, so a cross-site request has nothing to ride on. Recorded as
 *       a deliberate deviation in plan.md's Complexity Tracking.
 *   <li><b>Frame embedding denied</b>, because every action is unauthenticated and the board must
 *       not be wrapped by another site.
 *   <li><b>Content-Security-Policy {@code default-src 'self'}</b> with no inline script — free to
 *       satisfy, since the no-JavaScript baseline means there is no inline script to allow.
 * </ul>
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    private static final RequestMatcher API = new AntPathRequestMatcher("/api/**");

    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data:; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests
                        // Methods this application never uses are refused outright. These rules
                        // must precede anyRequest(), which Spring Security requires to be last.
                        .requestMatchers(HttpMethod.TRACE, "/**")
                        .denyAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .denyAll()
                        .anyRequest()
                        .permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers(API))
                .headers(headers -> headers.frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(policy -> policy.policyDirectives(CONTENT_SECURITY_POLICY)))
                // No login page, no logout handler, no user store: there are no accounts.
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .build();
    }
}
