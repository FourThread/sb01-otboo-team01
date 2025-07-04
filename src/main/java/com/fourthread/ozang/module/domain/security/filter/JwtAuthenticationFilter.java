package com.fourthread.ozang.module.domain.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.ErrorResponse;
import com.fourthread.ozang.module.domain.security.SecurityMatchers;
import com.fourthread.ozang.module.domain.security.UserDetailsImpl;
import com.fourthread.ozang.module.domain.security.jwt.dto.data.JwtPayloadDto;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

// Client가 HTTP 요청을 받을 때마다 인증 처리를 진행합니다
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;
  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain
  ) throws IOException, ServletException {
    log.info("[JwtAuthenticationFilter] 필터를 호출합니다 - URI: {}", request.getRequestURI());
    Optional<String> optionalAccessToken = resolveAccessToken(request);
    if (optionalAccessToken.isPresent() && !isPermitAll(request)) {
      String accessToken = optionalAccessToken.get();
      log.info("[JwtAuthenticationFilter] {} URI에서 AccessToken을 확인했습니다", request.getRequestURI());
      if (jwtService.validate(accessToken)) {
        JwtPayloadDto payloadDto = jwtService.parse(accessToken).payloadDto();
        log.info("[JwtAuthenticationFilter] 토큰이 유효합니다 - 사용자 : {}, 요청 URI : {}", payloadDto.email(), request.getRequestURI());
        UserDetailsImpl userDetails = new UserDetailsImpl(payloadDto, null);
        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.info("[JwtAuthenticationFilter] SecurityContext에 인증 완료 - 사용자: {}", payloadDto.email());

        long start = System.currentTimeMillis();
        chain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;

        log.info("[{}] {} 처리 시간: {}ms", request.getMethod(), request.getRequestURI(), duration);

      } else {
        log.warn("[JwtAuthFilter] 유효하지 않은 토큰 - 무효화 시도 - URI: {}", request.getRequestURI());
        jwtService.invalidateAccessToken(accessToken);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorDetails errorDetails = new ErrorDetails(
            "Authorization",
            "만료되었거나 서명이 올바르지 않은 JWT입니다"
        );

        ErrorResponse errorResponse = new ErrorResponse(
            "InvalidTokenException",
            "유효하지 않은 토큰입니다.",
            errorDetails
        );
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
      }
    } else {
      long start = System.currentTimeMillis();
      chain.doFilter(request, response);
      long duration = System.currentTimeMillis() - start;

      log.info("[{}] {} 처리 시간: {}ms", request.getMethod(), request.getRequestURI(), duration);
    }
  }

  private Optional<String> resolveAccessToken(HttpServletRequest request) {
    String prefix = "Bearer ";
    return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
        .map(value -> {
          if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
          } else {
            return null;
          }
        });
  }

  private boolean isPermitAll(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();

    for (String pattern : PUBLIC_PATTERNS) {
      PathPattern pathPattern = patternParser.parse(pattern);

      if (pattern.equals(SecurityMatchers.LOGIN) && !HttpMethod.POST.matches(method)) continue;
      if (pattern.equals(SecurityMatchers.LOGOUT) && !HttpMethod.POST.matches(method)) continue;
      if (pattern.equals(SecurityMatchers.SIGN_UP) && !HttpMethod.POST.matches(method)) continue;
      if (pattern.equals(SecurityMatchers.REFRESH) && !HttpMethod.POST.matches(method)) continue;
      if (pattern.equals(SecurityMatchers.ME) && !HttpMethod.GET.matches(method)) continue;

      if (pathPattern.matches(PathContainer.parsePath(path))) {
        return true;
      }
    }
    return false;
  }

  private static final String[] PUBLIC_PATTERNS = {
      SecurityMatchers.LOGIN,
      SecurityMatchers.LOGOUT,
      SecurityMatchers.H2_CONSOLE,
      SecurityMatchers.ME,
      SecurityMatchers.SIGN_UP,
      SecurityMatchers.REFRESH
  };

  private static final PathPatternParser patternParser = new PathPatternParser();
}
