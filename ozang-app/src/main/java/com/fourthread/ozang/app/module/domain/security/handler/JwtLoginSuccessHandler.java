package com.fourthread.ozang.module.domain.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.dto.response.JwtTokenResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Slf4j
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper;
  private final JwtService jwtService;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
    log.info("[JwtLoginSuccessHandler] 로그인 성공 - 사용자: {}", principal.getPayloadDto().email());
    log.info("[JwtLoginSuccessHandler] 이전 토큰을 무효화합니다");
    jwtService.invalidateJwtTokenByEmail(principal.getPayloadDto().email());

    JwtTokenResponse jwtSession = jwtService.registerJwtToken(principal.getPayloadDto());
    log.info("[JwtLoginSuccessHandler] 새로운 Access Token을 발급합니다");

    String refreshToken = jwtSession.refreshToken();
    Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    response.addCookie(refreshTokenCookie); // refresh token은 쿠키에 저장해서 반환

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(jwtSession.accessToken())); // Access Token은 응답 Body에 담아서 반환

  }
}
