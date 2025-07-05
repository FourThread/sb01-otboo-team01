package com.fourthread.ozang.module.domain.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.security.oauth.handler.OAuth2FailureHandler;
import com.fourthread.ozang.module.domain.security.oauth.handler.OAuth2SuccessHandler;
import com.fourthread.ozang.module.domain.security.oauth.service.CustomOAuth2UserService;
import com.fourthread.ozang.module.domain.security.filter.JsonLoginFilter.Configurer;
import com.fourthread.ozang.module.domain.security.handler.CustomAccessDeniedHandler;
import com.fourthread.ozang.module.domain.security.handler.CustomAuthenticationEntryPoint;
import com.fourthread.ozang.module.domain.security.handler.CustomLoginFailureHandler;
import com.fourthread.ozang.module.domain.security.jwt.dto.type.SecurityMatchers;
import com.fourthread.ozang.module.domain.security.filter.JwtAuthenticationFilter;
import com.fourthread.ozang.module.domain.security.handler.JwtLoginSuccessHandler;
import com.fourthread.ozang.module.domain.security.handler.JwtLogoutHandler;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, ObjectMapper objectMapper,
      JwtService jwtService,
      CustomAuthenticationEntryPoint authenticationEntryPoint,
      CustomAccessDeniedHandler accessDeniedHandler,
      CustomOAuth2UserService customOAuth2UserService,
      OAuth2SuccessHandler oAuth2SuccessHandler,
      OAuth2FailureHandler oAuth2FailureHandler
      ) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(request -> !request.getRequestURI().startsWith("/api/")).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityMatchers.SIGN_UP).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityMatchers.LOGIN).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityMatchers.LOGOUT).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityMatchers.REFRESH).permitAll()
            .requestMatchers(HttpMethod.GET, SecurityMatchers.ME).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityMatchers.RESET_PASSWORD).permitAll()
            .requestMatchers(HttpMethod.GET, SecurityMatchers.CSRF_TOKEN).permitAll()
            .requestMatchers(SecurityMatchers.H2_CONSOLE).permitAll()
            .requestMatchers(SecurityMatchers.OAUTH2).permitAll()
            .anyRequest().hasRole(Role.USER.name())
        )
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers(SecurityMatchers.H2_CONSOLE)
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
        )
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.disable())
        )
        .logout(logout ->
            logout
                .logoutUrl(SecurityMatchers.LOGOUT)
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
                .accessDeniedHandler(accessDeniedHandler))
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            .successHandler(oAuth2SuccessHandler)
            .failureHandler(oAuth2FailureHandler)
        );

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
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
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