package com.ozang.common.domain.user.dto.request;

import com.ozang.common.domain.user.dto.type.Role;

public record UserRoleUpdateRequest(
    Role role
) {

}
