package com.example.cloudfour.storeservice.config.security;

import com.example.cloudfour.modulecommon.filter.JwtClaimsAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Bean
    JwtClaimsAuthFilter jwtClaimsAuthFilter() {
        return new JwtClaimsAuthFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**", "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/stores/**","/menus/**","/reviews/**", "/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtClaimsAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
