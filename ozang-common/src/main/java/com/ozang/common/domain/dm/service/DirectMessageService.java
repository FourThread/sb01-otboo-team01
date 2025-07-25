package com.ozang.common.domain.dm.service;

import com.ozang.common.exception.ErrorCode;
import com.ozang.common.domain.dm.dto.DirectMessageCreateRequest;
import com.ozang.common.domain.dm.dto.DirectMessageDto;
import com.ozang.common.domain.dm.dto.DirectMessageDtoCursorRequest;
import com.ozang.common.domain.dm.dto.DirectMessageDtoCursorResponse;
import com.ozang.common.domain.dm.dto.DmItems;
import com.ozang.common.domain.dm.entity.DirectMessageURI;
import com.ozang.common.domain.dm.entity.DirectMessage;
import com.ozang.common.domain.dm.repository.DirectMessageRepository;
import com.ozang.common.domain.feed.entity.SortBy;
import com.ozang.common.domain.feed.entity.SortDirection;
import com.ozang.common.domain.notification.event.DmReceivedEvent;
import com.ozang.common.domain.notification.event.FollowingFeedCreatedEvent;
import com.ozang.common.domain.user.dto.data.UserSummary;
import com.ozang.common.domain.user.entity.User;
import com.ozang.common.domain.user.exception.UserException;
import com.ozang.common.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMessageService {

  private final DirectMessageRepository dmRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ApplicationEventPublisher eventPublisher;

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

    DirectMessageDto dmDto = getDirectMessageDto(sender, receiver, dm);

    messagingTemplate.convertAndSend(DirectMessageURI.SEND.getUri() + getDmKey(sender, receiver), dmDto);
    log.info("DM 전송: {}", dmDto);

    eventPublisher.publishEvent(new DmReceivedEvent(dmDto));

    return dmDto;
  }

  private DirectMessageDto getDirectMessageDto(User sender, User receiver, DirectMessage dm) {
    UserSummary senderSummary = new UserSummary(
        sender.getId(), sender.getName(), sender.getProfile().getProfileImageUrl());
    UserSummary receiverSummary = new UserSummary(
        receiver.getId(), receiver.getName(), receiver.getProfile().getProfileImageUrl());
    DirectMessageDto dmDto = new DirectMessageDto(
        dm.getId(),
        dm.getCreatedAt(),
        senderSummary,
        receiverSummary,
        dm.getContent()
    );
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

  /**
  * @methodName : retrieve
  * @date : 2025. 7. 4. PM 4:03
  * @author : wongil
  * @Description: DM 조회
  **/
  public DirectMessageDtoCursorResponse retrieve(DirectMessageDtoCursorRequest request) {

    List<DmItems> data = dmRepository.retrieveDm(request);
    Long totalCount = dmRepository.count(request);
    log.debug("Find All Direct Message count: {}", totalCount);

    Integer limit = request.limit();
    boolean hasNext = data.size() > limit;

    List<DmItems> pagedDirectMessages = hasNext ? data.subList(0, limit) : data;
    log.debug("페이징된 DM: {}", pagedDirectMessages.size());

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      DmItems lastData = pagedDirectMessages.get(pagedDirectMessages.size() - 1);
      nextCursor = lastData.createdAt().toString();
      nextIdAfter = lastData.id();
    }

    return DirectMessageDtoCursorResponse.builder()
        .data(pagedDirectMessages)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(hasNext)
        .totalCount(totalCount)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.ASCENDING)
        .build();
  }
}
