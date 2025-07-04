package com.fourthread.ozang.module.domain.security;

public class SecurityMatchers {

  public static final String LOGIN = "/api/auth/sign-in";
  public static final String LOGOUT = "/api/auth/sign-out";
  public static final String H2_CONSOLE = "/h2-console/**";
  public static final String ME = "/api/auth/me";
  public static final String SIGN_UP = "/api/users";
  public static final String REFRESH = "/api/auth/refresh";
  public static final String CSRF_TOKEN = "/api/auth/csrf-token";
}
