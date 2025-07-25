package com.ozang.web.notification.event;

import com.ozang.web.dm.dto.DirectMessageDto;

public record DmReceivedEvent(
        DirectMessageDto dmDto
) {
}
