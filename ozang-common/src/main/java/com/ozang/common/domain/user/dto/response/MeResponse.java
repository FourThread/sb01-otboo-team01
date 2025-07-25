package com.ozang.common.domain.user.dto.response;

import com.ozang.common.domain.user.dto.type.Role;
import java.util.UUID;

public record MeResponse(
    UUID userId,
    String email,
    String name,
    Role role
) {

}
