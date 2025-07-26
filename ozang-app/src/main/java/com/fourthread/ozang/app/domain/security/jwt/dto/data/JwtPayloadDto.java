package com.fourthread.ozang.app.domain.security.jwt.dto.data;

import com.fourthread.ozang.core.domain.user.dto.type.Role;
import com.fourthread.ozang.core.domain.user.entity.User;
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
