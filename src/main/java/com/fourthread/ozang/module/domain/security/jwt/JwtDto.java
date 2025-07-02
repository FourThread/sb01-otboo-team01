package com.fourthread.ozang.module.domain.security.jwt;

import com.fourthread.ozang.module.domain.user.dto.data.UserDto;
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
