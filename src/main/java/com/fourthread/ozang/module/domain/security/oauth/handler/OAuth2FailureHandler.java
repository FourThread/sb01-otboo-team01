package com.fourthread.ozang.module.domain.security.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {
  
  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {
    log.error("[OAuth2FailureHandler] OAuth 로그인 실패: {}", exception.getMessage());
    
    // 실패 원인에 따른 적절한 에러 메시지 설정
    String errorMessage = getErrorMessage(exception);
    
    // 홈 화면으로 리다이렉트하면서 에러 파라미터 추가
    String redirectUrl = "/?error=oauth_failed&message=" + java.net.URLEncoder.encode(errorMessage, "UTF-8");
    response.sendRedirect(redirectUrl);
  }
  
  private String getErrorMessage(AuthenticationException exception) {
    String message = exception.getMessage();
    
    // OAuth2 표준 에러 코드에 따른 메시지 처리
    if (message.contains("access_denied")) {
      return "소셜 로그인이 취소되었습니다.";
    } else if (message.contains("invalid_grant")) {
      return "인증이 만료되었습니다. 다시 시도해주세요.";
    } else if (message.contains("invalid_request")) {
      return "잘못된 요청입니다. 다시 시도해주세요.";
    } else if (message.contains("invalid_client")) {
      return "클라이언트 인증에 실패했습니다.";
    } else if (message.contains("invalid_scope")) {
      return "요청된 권한 범위가 유효하지 않습니다.";
    } else if (message.contains("server_error")) {
      return "소셜 로그인 서비스에 일시적인 문제가 있습니다.";
    } else if (message.contains("temporarily_unavailable")) {
      return "소셜 로그인 서비스가 일시적으로 사용할 수 없습니다.";
    } else if (message.contains("email")) {
      return "이메일 정보를 가져올 수 없습니다. 소셜 계정 설정을 확인해주세요.";
    } else {
      return "소셜 로그인에 실패했습니다. 다시 시도해주세요.";
    }
  }
}
