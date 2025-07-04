package com.fourthread.ozang.module.domain.dm.controller;

import com.fourthread.ozang.module.domain.dm.dto.DirectMessageCreateRequest;
import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDto;
import com.fourthread.ozang.module.domain.dm.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DirectMessageController {

  private final DirectMessageService dmMessageService;

  @MessageMapping("direct-messages_send")
  public DirectMessageDto send(@Payload DirectMessageCreateRequest request) {

    return dmMessageService.send(request);
  }
}
