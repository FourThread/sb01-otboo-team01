package com.fourthread.ozang.module.domain.dm.integration;


import static org.assertj.core.api.Assertions.assertThat;

import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDtoCursorRequest;
import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDtoCursorResponse;
import com.fourthread.ozang.module.domain.dm.entity.DirectMessage;
import com.fourthread.ozang.module.domain.dm.repository.DirectMessageRepository;
import com.fourthread.ozang.module.domain.dm.service.DirectMessageService;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class DirectMessageIntegrationTest {

  @Autowired
  private DirectMessageService dmService;
  @Autowired
  private DirectMessageRepository dmRepository;
  @Autowired
  private UserRepository userRepository;

  private User sender, receiver;
  private DirectMessage dm1, dm2, dm3;

  @BeforeEach
  void init() {
    sender = new User("sender", "sender@mail.com", "password1234");
    receiver = new User("receiver", "receiver@mail.com", "password1234");
    userRepository.save(sender);
    userRepository.save(receiver);

    dm1 = new DirectMessage(sender, receiver, "dm1",
        DirectMessage.generatedDmKey(sender, receiver));
    dm2 = new DirectMessage(sender, receiver, "dm2",
        DirectMessage.generatedDmKey(sender, receiver));
    dm3 = new DirectMessage(sender, receiver, "dm3",
        DirectMessage.generatedDmKey(sender, receiver));
    dmRepository.save(dm1);
    dmRepository.save(dm2);
    dmRepository.save(dm3);
  }

  @Test
  @DisplayName("DM 조회 - 페이징 안함")
  void find() {
    DirectMessageDtoCursorRequest request = new DirectMessageDtoCursorRequest(sender.getId(), null,
        null, 2);

    DirectMessageDtoCursorResponse response = dmService.retrieve(request);

    assertThat(response.data().size()).isEqualTo(2);
    assertThat(response.data().get(0).content()).isEqualTo("dm1");

    assertThat(response.nextCursor()).isNotNull();
    assertThat(response.nextIdAfter()).isNotNull();
    assertThat(response.totalCount()).isEqualTo(3);
  }

//  @Test
//  @DisplayName("DM 조회 - 페이징")
//  void paging() {
//    DirectMessageDtoCursorRequest nonePagingRequest = new DirectMessageDtoCursorRequest(sender.getId(), null, null, 2);
//    DirectMessageDtoCursorResponse nonePagedResponse = dmService.retrieve(nonePagingRequest);
//
//    DirectMessageDtoCursorRequest pagingRequest = new DirectMessageDtoCursorRequest(sender.getId(),
//        nonePagedResponse.nextCursor(), nonePagedResponse.nextIdAfter(), 2);
//    DirectMessageDtoCursorResponse pagedResponse = dmService.retrieve(pagingRequest);
//
//    assertThat(pagedResponse.data().size()).isEqualTo(1);
//    assertThat(pagedResponse.data().get(0).content()).isEqualTo("dm3");
//
//    assertThat(pagedResponse.nextIdAfter()).isNull();
//    assertThat(pagedResponse.nextCursor()).isNull();
//  }
}
