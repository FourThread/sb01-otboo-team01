package com.fourthread.ozang.module.domain.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.security.jwt.dto.type.SecurityMatchers;
import com.fourthread.ozang.module.domain.user.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Slf4j
@RequiredArgsConstructor
public class JsonLoginFilter extends UsernamePasswordAuthenticationFilter {

  private final ObjectMapper objectMapper;

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws AuthenticationException {

    if (!request.getMethod().equals("POST")) {
      log.warn("[JsonLoginFilter] 지원하지 않는 메서드 요청 : {}", request.getMethod());
      throw new AuthenticationServiceException("지원하지 않는 메서드입니다! : " + request.getMethod());
    }

    try {
      LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(),
          LoginRequest.class);
      log.info("[JsonLoginFilter] 로그인을 시도합니다 email: {}", loginRequest.email());

      UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());

      setDetails(request, token);
      log.info("[JsonLoginFilter] 인증 매니저를 통해 인증 시도 완료");
      return this.getAuthenticationManager().authenticate(token);

    } catch (IOException e) {
      log.error("[JsonLoginFilter] Request Body Parsing failed : {}", e.getMessage());
      throw new AuthenticationServiceException("요청 내용을 확인하지 못했습니다 : " + e.getMessage());
    }
  }

  public static JsonLoginFilter createDefault(
      ObjectMapper objectMapper,
      AuthenticationManager authenticationManager
  ) {

    JsonLoginFilter filter = new JsonLoginFilter(objectMapper);

    PathPatternRequestMatcher matcher = PathPatternRequestMatcher.withDefaults()
        .matcher(HttpMethod.POST, SecurityMatchers.LOGIN);
    filter.setRequiresAuthenticationRequestMatcher(matcher);
    filter.setAuthenticationManager(authenticationManager);
    return filter;
  }

  public static class Configurer extends
      AbstractAuthenticationFilterConfigurer<HttpSecurity, Configurer, JsonLoginFilter> {

    public Configurer(ObjectMapper objectMapper) {
      super(new JsonLoginFilter(objectMapper), SecurityMatchers.LOGIN);
    }

    @Override
    protected RequestMatcher createLoginProcessingUrlMatcher(String loginProcessingUrl) {
      return PathPatternRequestMatcher.withDefaults()
          .matcher(HttpMethod.POST, loginProcessingUrl);
    }

    @Override
    public void init(HttpSecurity http) throws Exception {
      loginProcessingUrl(SecurityMatchers.LOGIN);
    }
  }
}
