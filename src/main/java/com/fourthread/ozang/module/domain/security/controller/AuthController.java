package com.fourthread.ozang.module.domain.security.controller;

import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.JwtToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@RestController
public class AuthController {

  private final JwtService jwtService;

  @GetMapping("/me")
  public ResponseEntity<String> me(
      @CookieValue(value = "refresh-token") String refreshToken) {
    JwtToken jwtToken = jwtService.getJwtToken(refreshToken);
    return ResponseEntity.status(HttpStatus.OK).body(jwtToken.getAccessToken());
  }

  @PostMapping("/refresh")
  public ResponseEntity<String> refresh(
      @CookieValue(value = "refresh-token") String refreshToken,
      HttpServletResponse response
  ) {
    JwtToken jwtSession = jwtService.refreshJwtToken(refreshToken);

    Cookie refreshTokenCookie = new Cookie("refresh-token",
        jwtSession.getRefreshToken());
    refreshTokenCookie.setHttpOnly(true);
    response.addCookie(refreshTokenCookie);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(jwtSession.getAccessToken());
  }
}