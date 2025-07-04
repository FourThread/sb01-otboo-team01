package com.fourthread.ozang.module.domain.security.controller;

import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.JwtToken;
import com.fourthread.ozang.module.domain.security.jwt.dto.response.JwtTokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import com.fourthread.ozang.module.domain.user.dto.request.ResetPasswordRequest;
import com.fourthread.ozang.module.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    JwtTokenResponse jwtToken = jwtService.refreshJwtToken(refreshToken);
    return ResponseEntity.status(HttpStatus.OK).body(jwtToken.accessToken());
  }

  // 리프레시 토큰을 이용해서 리프레시 토큰과 엑세스 토큰을 재발급 받는다
  @PostMapping("/refresh")
  public ResponseEntity<String> refresh(
      @CookieValue(value = "refresh_token") String refreshToken,
      HttpServletResponse response
  ) {
    log.info("[AuthController] Refresh 토큰 요청 수신");
    JwtTokenResponse jwtToken = jwtService.refreshJwtToken(refreshToken);

    log.info("[AutnController] AccessToken 재발급 완료!");
    Cookie refreshTokenCookie = new Cookie("refresh_token",
        jwtToken.refreshToken());
    refreshTokenCookie.setHttpOnly(true);
    response.addCookie(refreshTokenCookie);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(jwtToken.accessToken());
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @RequestBody @Valid ResetPasswordRequest request
  ) {
    userService.resetPassword(request.email());
    return ResponseEntity.noContent().build();
  }
}