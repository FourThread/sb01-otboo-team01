package com.fourthread.ozang.domain.security.jwt.dto.data;

import com.fourthread.ozang.module.domain.security.jwt.dto.data.JwtPayloadDto;
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
