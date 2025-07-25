package com.fourthread.ozang.module.domain.security.jwt.dto.data;

import java.time.Instant;

public record JwtDto(
    Instant iat,
    Instant exp,
    JwtPayloadDto payloadDto,
    String token
) {

  public boolean isExpired() {
    return exp.isBefore(Instant.now());
  }
}
