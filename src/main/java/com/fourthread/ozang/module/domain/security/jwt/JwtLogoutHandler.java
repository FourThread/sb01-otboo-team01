package com.fourthread.ozang.module.domain.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

@Slf4j
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

  private final JwtService jwtService;

  //CheckedException을 명시적으로 throws 선언하지 않고도 던질 수 있게 해준다
  @SneakyThrows
  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    log.info("[JwtLogoutHandler] 로그아웃 요청 수신");
    resolveRefreshToken(request)
        .ifPresentOrElse(refreshToken -> {
          log.info("[JwtLogoutHandler] 리프레시 토큰 쿠키 발견: {}", refreshToken);
          jwtService.invalidateJwtToken(refreshToken);
          log.info("[JwtLogoutHandler] JWT 토큰 무효화 완료");
          invalidateRefreshTokenCookie(response);
          log.info("[JwtLogoutHandler] 리프레시 토큰 쿠키 삭제 완료");
        }, () -> {
          log.warn("[JwtLogoutHandler] 리프레시 토큰 쿠키가 존재하지 않음");
        });
  }

  private Optional<String> resolveRefreshToken(HttpServletRequest request) {
    return Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equals("refresh_token"))
        .findFirst()
        .map(Cookie::getValue);
  }

  private void invalidateRefreshTokenCookie(HttpServletResponse response) {
    Cookie refreshTokenCookie = new Cookie("refresh_token", "");
    refreshTokenCookie.setMaxAge(0);
    refreshTokenCookie.setHttpOnly(true);
    response.addCookie(refreshTokenCookie);
  }
}