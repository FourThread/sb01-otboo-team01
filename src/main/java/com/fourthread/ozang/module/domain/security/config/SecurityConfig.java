package com.fourthread.ozang.module.domain.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.security.filter.JsonLoginFilter.Configurer;
import com.fourthread.ozang.module.domain.security.handler.CustomAccessDeniedHandler;
import com.fourthread.ozang.module.domain.security.handler.CustomAuthenticationEntryPoint;
import com.fourthread.ozang.module.domain.security.handler.CustomLoginFailureHandler;
import com.fourthread.ozang.module.domain.security.SecurityMatchers;
import com.fourthread.ozang.module.domain.security.filter.JsonLoginFilter;
import com.fourthread.ozang.module.domain.security.filter.JwtAuthenticationFilter;
import com.fourthread.ozang.module.domain.security.jwt.JwtLoginSuccessHandler;
import com.fourthread.ozang.module.domain.security.jwt.JwtLogoutHandler;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyAuthoritiesMapper;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, ObjectMapper objectMapper,
      DaoAuthenticationProvider daoAuthenticationProvider,
      JwtService jwtService,
      CustomAuthenticationEntryPoint authenticationEntryPoint,
      CustomAccessDeniedHandler accessDeniedHandler
      ) throws Exception {
    http
        .authenticationProvider(daoAuthenticationProvider)
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(SecurityMatchers.PUBLIC_MATCHERS).permitAll()
            .anyRequest().hasRole(Role.USER.name())
        )
        .csrf(csrf -> csrf.disable()
        )
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.disable())
        )
        .logout(logout ->
            logout
                .logoutRequestMatcher(SecurityMatchers.LOGOUT)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                .addLogoutHandler(new JwtLogoutHandler(jwtService))
        )
        .with(
            new Configurer(objectMapper),
            configurer ->
                configurer
                    .successHandler(new JwtLoginSuccessHandler(objectMapper, jwtService))
                    .failureHandler(new CustomLoginFailureHandler(objectMapper))
        )
        .sessionManagement(session ->
            session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(new JwtAuthenticationFilter(objectMapper, jwtService),
            UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(exceptionHandler ->
            exceptionHandler.authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      RoleHierarchy roleHierarchy
  ) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    provider.setAuthoritiesMapper(new RoleHierarchyAuthoritiesMapper(roleHierarchy));
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      List<AuthenticationProvider> authenticationProviders) {
    return new ProviderManager(authenticationProviders);
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role(Role.ADMIN.name())
        .implies(Role.USER.name())
        .build();
  }
}