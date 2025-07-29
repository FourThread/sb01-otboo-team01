package com.fourthread.ozang.app.domain.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.core.common.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Slf4j
@RequiredArgsConstructor
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    log.info("[CustomLoginFailureHandler] 로그인에 실패했습니다: {}", exception.getMessage());

    // 로그인 실패 시 홈(/)으로 리디렉트
    response.sendRedirect("/");
  }
}
