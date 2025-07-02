package com.fourthread.ozang.module.domain.dm.service;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.domain.dm.dto.DirectMessageCreateRequest;
import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDto;
import com.fourthread.ozang.module.domain.dm.entity.DirectMessageURI;
import com.fourthread.ozang.module.domain.dm.entity.DirectMessage;
import com.fourthread.ozang.module.domain.dm.repository.DirectMessageRepository;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMessageService {

  private final DirectMessageRepository dmRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  /**
  * @methodName : send
  * @date : 2025-07-02 오후 2:19
  * @author : wongil
  * @Description: DM 보내기
  **/
  public DirectMessageDto send(DirectMessageCreateRequest request) {

    User sender = getUser(request.senderId());
    User receiver = getUser(request.receiverId());

    DirectMessage dm = createDirectMessage(request, sender, receiver);
    dmRepository.save(dm);
    log.info("DM 생성 완료: {}", dm.getId());

    DirectMessageDto dmDto = new DirectMessageDto(sender.getId(), receiver.getId(),
        request.content());

    messagingTemplate.convertAndSend(DirectMessageURI.SEND.getUri() + getDmKey(sender, receiver), dmDto);
    log.info("DM 전송: {}", dmDto);

    return dmDto;
  }

  private String getDmKey(User sender, User receiver) {
    return DirectMessage.generatedDmKey(sender, receiver);
  }

  private DirectMessage createDirectMessage(DirectMessageCreateRequest request, User sender, User receiver) {
    return DirectMessage.builder()
        .sender(sender)
        .receiver(receiver)
        .content(request.content())
        .dmKey(getDmKey(sender, receiver))
        .build();
  }

  private User getUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(
            () -> new UserException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage(),
                this.getClass().getSimpleName()));
  }
}
