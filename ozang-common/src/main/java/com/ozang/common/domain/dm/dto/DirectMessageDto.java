package com.ozang.common.domain.dm.dto;

import com.ozang.common.domain.user.dto.data.UserSummary;
import java.time.LocalDateTime;
import java.util.UUID;

public record DirectMessageDto (

    UUID id,
    LocalDateTime createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content

) {

}
