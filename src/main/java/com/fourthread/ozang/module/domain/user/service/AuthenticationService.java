package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.security.dto.JwtToken;
import com.fourthread.ozang.module.domain.user.dto.request.LoginRequest;

public interface AuthenticationService {

  JwtToken signIn(LoginRequest request);

  void logout(String refreshToken);
}
