package com.ozang.web.user.dto.response;

import com.ozang.web.user.dto.type.Role;
import java.util.UUID;

public record MeResponse(
    UUID userId,
    String email,
    String name,
    Role role
) {

}
