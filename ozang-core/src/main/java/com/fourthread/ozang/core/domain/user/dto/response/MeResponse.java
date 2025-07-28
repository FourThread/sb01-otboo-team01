package com.fourthread.ozang.core.domain.user.dto.response;

import com.fourthread.ozang.core.domain.user.dto.type.Role;
import java.util.UUID;

public record MeResponse(
    UUID userId,
    String email,
    String name,
    Role role
) {

}
