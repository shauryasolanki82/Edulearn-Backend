package com.edulearn.course.config;

import com.edulearn.course.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public read-only endpoints (no login needed)
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/courses",
                    "/api/v1/courses/search",
                    "/api/v1/courses/featured",
                    "/api/v1/courses/top",
                    "/api/v1/courses/free",
                    "/api/v1/courses/{id}",
                    "/api/v1/courses/category/**"
                ).permitAll()
                // Swagger / Actuator
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/api-docs/**",
                    "/actuator/health", "/actuator/info"
                ).permitAll()
                // Internal calls (enrollment/lesson service updating counts)
                .requestMatchers("/api/v1/courses/internal/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
