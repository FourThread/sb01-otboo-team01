package com.fourthread.ozang.module.domain.dm.dto;

import java.util.UUID;

public record DirectMessageDto (

    UUID senderId,
    UUID receiverId,
    String content

) {

}
