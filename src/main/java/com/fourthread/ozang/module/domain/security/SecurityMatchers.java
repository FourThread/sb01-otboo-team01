package com.fourthread.ozang.module.domain.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class SecurityMatchers {

  public static final RequestMatcher NON_API = new NegatedRequestMatcher(
      new AntPathRequestMatcher("/api/**"));
  public static final RequestMatcher SIGN_UP = new AntPathRequestMatcher(
      "/api/users", HttpMethod.POST.name());
  public static final RequestMatcher LOGIN = new AntPathRequestMatcher(
      "/api/auth/sign-in", HttpMethod.POST.name());
  public static final String LOGIN_URL = "/api/auth/login";
  public static final RequestMatcher LOGOUT = new AntPathRequestMatcher(
      "/api/auth/sign-out", HttpMethod.POST.name());
  public static final RequestMatcher ME = new AntPathRequestMatcher(
      "/api/auth/me", HttpMethod.GET.name()
  );
  public static final RequestMatcher REFRESH = new AntPathRequestMatcher(
      "/api/auth/refresh", HttpMethod.POST.name()
  );

  public static final RequestMatcher[] PUBLIC_MATCHERS = new RequestMatcher[]{
      NON_API, SIGN_UP, LOGIN, LOGOUT, ME, REFRESH
  };

}
