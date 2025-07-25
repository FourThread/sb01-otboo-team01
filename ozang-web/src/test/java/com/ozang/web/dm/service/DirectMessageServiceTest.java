package com.fourthread.ozang.module.domain.dm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDtoCursorRequest;
import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDtoCursorResponse;
import com.fourthread.ozang.module.domain.dm.dto.DmItems;
import com.fourthread.ozang.module.domain.dm.repository.DirectMessageRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class DirectMessageServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private DirectMessageRepository dmRepository;

  @Mock
  private SimpMessagingTemplate simpMessagingTemplate;

  @InjectMocks
  private DirectMessageService dmService;

  UUID sender = UUID.randomUUID();
  UUID receiver = UUID.randomUUID();

  DmItems dm1 = new DmItems(sender, LocalDateTime.now(),
      new UserSummary(sender, "sender", null),
      new UserSummary(receiver, "receiver", null), "dm1");

  DmItems dm2 = new DmItems(sender, LocalDateTime.now(),
      new UserSummary(sender, "sender", null),
      new UserSummary(receiver, "receiver", null), "dm2");

  DmItems dm3 = new DmItems(sender, LocalDateTime.now(),
      new UserSummary(sender, "sender", null),
      new UserSummary(receiver, "receiver", null), "dm3");

  @Test
  @DisplayName("DM 목록 조회 - 페이징 안함")
  void find() {
    DirectMessageDtoCursorRequest request = new DirectMessageDtoCursorRequest(
        UUID.randomUUID(), null, null, 2);

    List<DmItems> data = List.of(dm1, dm2, dm3);

    when(dmRepository.retrieveDm(any()))
        .thenReturn(data);

    DirectMessageDtoCursorResponse response = dmService.retrieve(request);

    assertThat(response.data().size()).isEqualTo(2);
    assertThat(response.data().get(0).content()).isEqualTo("dm1");
    assertThat(response.data().get(0).sender().userId()).isEqualTo(sender);

    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
    assertThat(response.nextIdAfter()).isNotNull();
  }

  @Test
  @DisplayName("DM 목록 조회 - 페이징함")
  void paging() {
    // given
    LocalDateTime now = LocalDateTime.now();
    DmItems dm1 = new DmItems(sender, now.minusMinutes(3),
        new UserSummary(sender, "sender", null),
        new UserSummary(receiver, "receiver", null), "dm1");

    DmItems dm2 = new DmItems(sender, now.minusMinutes(2),
        new UserSummary(sender, "sender", null),
        new UserSummary(receiver, "receiver", null), "dm2");

    DmItems dm3 = new DmItems(sender, now.minusMinutes(1),
        new UserSummary(sender, "sender", null),
        new UserSummary(receiver, "receiver", null), "dm3");

    // when
    DirectMessageDtoCursorRequest nonePagingRequest = new DirectMessageDtoCursorRequest(
        UUID.randomUUID(), null, null, 2);
    List<DmItems> data = List.of(dm1, dm2, dm3);
    when(dmRepository.retrieveDm(any()))
        .thenReturn(data);
    DirectMessageDtoCursorResponse nonePagedResponse = dmService.retrieve(nonePagingRequest);

    // when
    // 페이징
    DirectMessageDtoCursorRequest pagingRequest = new DirectMessageDtoCursorRequest(
        UUID.randomUUID(), nonePagedResponse.nextCursor(), nonePagedResponse.nextIdAfter(), 2);
    when(dmRepository.retrieveDm(any()))
        .thenReturn(List.of(dm3));
    DirectMessageDtoCursorResponse pagedResponse = dmService.retrieve(pagingRequest);

    // then
    assertThat(pagedResponse.data().size()).isEqualTo(1);
    assertThat(pagedResponse.data().get(0).content()).isEqualTo("dm3");
    assertThat(pagedResponse.nextCursor()).isNull();
    assertThat(pagedResponse.nextIdAfter()).isNull();
  }
}