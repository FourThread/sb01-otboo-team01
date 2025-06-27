package com.fourthread.ozang.module.domain.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;
  @Value("${jwt.access-token-expiration-ms}")
  private long accessTokenValiditySeconds;
  @Value("${jwt.refresh-token-expiration-ms}")
  private long refreshTokenValiditySeconds;

}
