package com.fourthread.ozang.domain.user.dto.request;

import com.fourthread.ozang.domain.user.dto.type.Role;

public record UserRoleUpdateRequest(
    Role role
) {

}
