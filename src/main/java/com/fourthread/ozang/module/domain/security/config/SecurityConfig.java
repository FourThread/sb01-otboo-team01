package com.fourthread.ozang.module.domain.security.config;

import com.fourthread.ozang.module.domain.security.filter.JwtAuthenticationFilter;
import com.fourthread.ozang.module.domain.security.provider.JwtTokenProvider;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * API 엔드포인트 경로 상수
   */
  private static class ApiEndpoints {
    // 인증 관련 엔드포인트
    private static final String AUTH_ME = "/api/auth/me";
    private static final String AUTH_LOGIN = "/api/auth/sign-in";
    private static final String AUTH_LOGOUT = "/api/auth/sign-out";
    private static final String RESET_PASSWORD = "/api/auth/reset-password";
    private static final String REFRESH = "/api/auth/refresh";

    // 사용자 관련 엔드포인트
    private static final String USERS = "/api/users";
  }

  /**
   * 정적 리소스 경로 상수
   */
  private static class StaticResources {
    private static final String[] SWAGGER_RESOURCES = {
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-resources/**"
    };

    private static final String[] PUBLIC_RESOURCES = {
        "/actuator/**",
        "/favicon.ico",
        "/",
        "/assets/**",
        "/index.html",
        "/closet-hanger-logo.png"
    };
  }

  /**
   * 권한 상수
   */
  private static class Roles {
    private static final String USER = "USER";
    private static final String ADMIN = "ADMIN";

    private static String hasRole(String role) {
      return "ROLE_" + role;
    }
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf().disable()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(StaticResources.SWAGGER_RESOURCES).permitAll()
            .requestMatchers(StaticResources.PUBLIC_RESOURCES).permitAll()
            .requestMatchers(HttpMethod.POST, ApiEndpoints.USERS).permitAll()
            .requestMatchers(HttpMethod.POST, ApiEndpoints.AUTH_LOGIN).permitAll()
            .anyRequest().hasRole(Roles.USER)
        )
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
        .build();

  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


}
