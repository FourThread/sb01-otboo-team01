package com.fourthread.ozang.domain.user.dto.data;

import java.util.UUID;

public record UserSummary(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
