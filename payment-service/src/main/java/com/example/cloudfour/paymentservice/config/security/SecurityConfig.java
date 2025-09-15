package com.example.cloudfour.paymentservice.config.security;

import com.example.cloudfour.modulecommon.passport.filter.InternalPassportFilter;
import com.example.cloudfour.modulecommon.util.PassportUtil;
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
    InternalPassportFilter internalPassportFilter(PassportUtil passportUtil) {
        return new InternalPassportFilter(passportUtil);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, InternalPassportFilter internalPassportFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/payments/webhook").permitAll()
                        .requestMatchers("/internal/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalPassportFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
