package com.fourthread.ozang.app.domain.security.oauth.handler;

import com.fourthread.ozang.app.domain.security.jwt.JwtService;
import com.fourthread.ozang.app.domain.security.jwt.dto.data.JwtPayloadDto;
import com.fourthread.ozang.app.domain.security.jwt.dto.response.JwtTokenResponse;
import com.fourthread.ozang.core.domain.user.entity.User;
import com.fourthread.ozang.core.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException, ServletException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = extractEmail(oAuth2User.getAttributes());
    log.info("[OAuth2SuccessHandler] 추출된 이메일: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("OAuth2 인증된 사용자가 데이터베이스에 존재하지 않습니다: " + email));

    log.info("[OAuth2SuccessHandler] OAuth2 로그인 성공 - 사용자: {}", email);

    // JWT 토큰 생성
    JwtPayloadDto payloadDto = new JwtPayloadDto(user.getId(), user.getEmail(), user.getName(), user.getRole());

    JwtTokenResponse jwtSession = jwtService.registerJwtToken(payloadDto);
    log.info("[OAuth2SuccessHandler] 새로운 Access Token을 발급합니다");

    String refreshToken = jwtSession.refreshToken();
    Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setPath("/");
    response.addCookie(refreshTokenCookie);

    String accessToken = jwtSession.accessToken();
    Cookie accessTokenCookie = new Cookie("access_token", accessToken);
    accessTokenCookie.setHttpOnly(false);
    accessTokenCookie.setPath("/");
    response.addCookie(accessTokenCookie);

    String redirectUrl = "/#/";
    response.sendRedirect(redirectUrl);
  }

  private String extractEmail(Map<String, Object> attributes) {
    if (attributes.get("kakao_account") instanceof Map<?, ?> kakaoAccount) {
      Object email = kakaoAccount.get("email");
      if (email instanceof String e) return e;
    }
    if (attributes.containsKey("email")) {
      return (String) attributes.get("email");
    }
    return "unknown@example.com";
  }
}