package com.fourthread.ozang.module.domain.weather.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        return http
//            .authorizeHttpRequests(auth -> auth
//                // 날씨 API는 인증 없이 접근 허용
//                .requestMatchers("/api/weathers/**").permitAll()
//                .requestMatchers("/test/weathers/**").permitAll()
//                .requestMatchers("/h2-console/**").permitAll()  // H2 콘솔도 허용
//                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger도 허용
//                // 나머지는 인증 필요
//                .anyRequest().authenticated()
//            )
//            .csrf(csrf -> csrf
//                .ignoringRequestMatchers("/api/weathers/**")  // 날씨 API는 CSRF 비활성화
//                .ignoringRequestMatchers("/h2-console/**")    // H2 콘솔 CSRF 비활성화
//            )
//            .build();
//    }
//}
