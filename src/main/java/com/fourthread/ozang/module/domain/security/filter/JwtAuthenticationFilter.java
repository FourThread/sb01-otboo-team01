package com.fourthread.ozang.module.domain.security.filter;

import com.fourthread.ozang.module.domain.security.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String token = resolveToken(request);
    String requestURI = request.getRequestURI();

    if (StringUtils.hasText(token)) {
      log.debug("요청 URI: {}, Authorization 헤더에서 추출한 토큰 있음", requestURI);

      if (jwtTokenProvider.validateToken(token)) {
        try {
          Authentication authentication = jwtTokenProvider.getAuthentication(token);
          SecurityContextHolder.getContext().setAuthentication(authentication);
          log.debug("JWT 인증 성공: 사용자 = {}", authentication.getName());
        } catch (Exception ex) {
          log.warn("JWT 인증 실패: {}", ex.getMessage());
        }
      } else {
        log.warn("유효하지 않은 JWT 토큰 - URI: {}", requestURI);
      }

    } else {
      log.debug("Authorization 헤더에 토큰 없음 - URI: {}", requestURI);
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}