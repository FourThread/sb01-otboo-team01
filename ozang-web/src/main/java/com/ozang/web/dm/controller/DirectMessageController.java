package com.ozang.web.dm.controller;

import com.ozang.web.dm.dto.DirectMessageCreateRequest;
import com.ozang.web.dm.dto.DirectMessageDto;
import com.ozang.web.dm.dto.DirectMessageDtoCursorRequest;
import com.ozang.web.dm.dto.DirectMessageDtoCursorResponse;
import com.ozang.web.dm.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DirectMessageController {

  private final DirectMessageService dmMessageService;

  @MessageMapping("direct-messages_send")
  public DirectMessageDto send(@Payload DirectMessageCreateRequest request) {

    return dmMessageService.send(request);
  }
  
  @GetMapping("/api/direct-messages")
  public DirectMessageDtoCursorResponse retrieveDirectMessage(
      @Validated @ModelAttribute DirectMessageDtoCursorRequest request) {

    return dmMessageService.retrieve(request);

  }
}
