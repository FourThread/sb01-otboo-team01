package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.dm.dto.DirectMessageDto;

public record DmReceivedEvent(
        DirectMessageDto dmDto
) {
}
