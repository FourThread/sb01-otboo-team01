package com.fourthread.ozang.module.domain.user.service.impl;

import com.fourthread.ozang.module.domain.security.dto.JwtToken;
import com.fourthread.ozang.module.domain.security.jwt.RefreshToken;
import com.fourthread.ozang.module.domain.security.jwt.RefreshTokenRepository;
import com.fourthread.ozang.module.domain.security.provider.JwtTokenProvider;
import com.fourthread.ozang.module.domain.user.dto.request.LoginRequest;
import com.fourthread.ozang.module.domain.user.service.AuthenticationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManagerBuilder authenticationManagerBuilder;

  @Override
  public JwtToken signIn(LoginRequest request) {
    String email = request.email();
    String password = request.password();
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
        email, password);

    Authentication authentication = authenticationManagerBuilder.getObject()
        .authenticate(authenticationToken);

    JwtToken token = jwtTokenProvider.generateToken(authentication);

    RefreshToken refreshToken = new RefreshToken(email, token.refreshToken(),
        LocalDateTime.now().plus(
            Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpirationMs())));

    refreshTokenRepository.save(refreshToken);

    return token;
  }

  @Override
  public void logout(String accessToken) {
    Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

    String email = authentication.getName();
    refreshTokenRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("토큰을 찾을 수 없습니다"));

    refreshTokenRepository.deleteByEmail(email);
  }
}