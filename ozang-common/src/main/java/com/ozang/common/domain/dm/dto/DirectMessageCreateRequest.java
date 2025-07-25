package com.ozang.common.domain.dm.dto;

import java.util.UUID;

public record DirectMessageCreateRequest (

    UUID receiverId,
    UUID senderId,
    String content

) {

}
