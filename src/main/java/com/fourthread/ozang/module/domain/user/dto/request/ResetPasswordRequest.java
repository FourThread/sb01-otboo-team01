package com.fourthread.ozang.module.domain.user.dto.request;

import jakarta.validation.constraints.Email;

public record ResetPasswordRequest(
    @Email
    String email
) {

}
