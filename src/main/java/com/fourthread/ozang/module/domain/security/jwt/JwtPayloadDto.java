package com.fourthread.ozang.module.domain.security.jwt;

import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.User;
import java.util.UUID;

public record JwtPayloadDto(
    UUID userId,
    String email,
    String name,
    Role role
) {

  public static JwtPayloadDto toJwtPayloadDto(User user) {
    return new JwtPayloadDto(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getRole()
    );
  }
}
