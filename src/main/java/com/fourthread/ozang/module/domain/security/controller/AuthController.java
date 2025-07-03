package com.fourthread.ozang.module.domain.security.controller;

import com.fourthread.ozang.module.domain.security.CsrfTokenResponse;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.JwtToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import com.fourthread.ozang.module.domain.user.dto.request.ResetPasswordRequest;
import com.fourthread.ozang.module.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController {

  private final JwtService jwtService;
  private final UserService userService;

  // 리프레시 토큰을 이용해서 엑세스 토큰을 조회
  @GetMapping("/me")
  public ResponseEntity<String> me(
      @CookieValue(value = "refresh_token") String refreshToken) {
    JwtToken jwtToken = jwtService.getJwtToken(refreshToken);
    return ResponseEntity.status(HttpStatus.OK).body(jwtToken.getAccessToken());
  }

  // 리프레시 토큰을 이용해서 리프레시 토큰과 엑세스 토큰을 재발급 받는다
  @PostMapping("/refresh")
  public ResponseEntity<String> refresh(
      @CookieValue(value = "refresh_token") String refreshToken,
      HttpServletResponse response
  ) {
    log.info("[AuthController] Refresh 토큰 요청 수신");
    JwtToken jwtSession = jwtService.refreshJwtToken(refreshToken);

    log.info("[AutnController] AccessToken 재발급 완료! - 사용자 : {}, 만료 시간 : {}", jwtSession.getEmail(),
        jwtSession.getExpiryDate());
    Cookie refreshTokenCookie = new Cookie("refresh_token",
        jwtSession.getRefreshToken());
    refreshTokenCookie.setHttpOnly(true);
    response.addCookie(refreshTokenCookie);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(jwtSession.getAccessToken());
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @RequestBody @Valid ResetPasswordRequest request
  ) {
    userService.resetPassword(request.email());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/csrf-token")
  public ResponseEntity<CsrfTokenResponse> getCsrfToken(CsrfToken csrfToken, HttpServletResponse response) {
    log.debug("[CSRF] CsrfToken 토큰을 발급합니다.");
    ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", csrfToken.getToken())
        .httpOnly(false)
        .path("/")
        .secure(false)
        .sameSite("Lax")
        .build();

    return ResponseEntity.ok(
        new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken(), csrfToken.getParameterName())
    );
  }
}