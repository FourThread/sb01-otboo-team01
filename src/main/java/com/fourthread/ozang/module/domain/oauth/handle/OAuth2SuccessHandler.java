package com.fourthread.ozang.module.domain.oauth.handle;

import com.fourthread.ozang.module.domain.security.jwt.JwtService;
import com.fourthread.ozang.module.domain.security.jwt.JwtToken;
import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final UserMapper userMapper;

  @Transactional
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {
    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String email = oAuth2User.getAttribute("email");

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("OAuth2 로그인된 사용자 정보가 없습니다."));

    UserDto userDto = userMapper.toDto(user);

    JwtToken jwtToken = jwtService.registerJwtToken(userDto);

    String redirectUri = UriComponentsBuilder
        .fromUriString("http://localhost:8080")
        .queryParam("accessToken", jwtToken.getAccessToken())
        .queryParam("refreshToken", jwtToken.getRefreshToken())
        .build().toUriString();

    response.sendRedirect(redirectUri);
  }
}
