package com.fourthread.ozang.module.domain.security.oauth.handle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {
  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    if (exception.getCause() instanceof LockedException) {
      log.error("[OAuth2FailureHandler] 잠긴 계정으로 OAuth 로그인 시도: {}", exception.getMessage());
    } else {
      log.error("[OAuth2FailureHandler] OAuth 로그인 실패: {}", exception.getMessage());
    }
    response.sendRedirect("/");
  }
}
