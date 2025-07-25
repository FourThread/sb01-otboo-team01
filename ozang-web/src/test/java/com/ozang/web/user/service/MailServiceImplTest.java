package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.user.service.impl.AsyncMailSender;
import com.fourthread.ozang.module.domain.user.service.impl.MailServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

  @Mock
  AsyncMailSender asyncMailSender;

  @InjectMocks
  MailServiceImpl mailService;

  @Test
  @DisplayName("임시 비밀번호 메일 전송 시 AsyncMailSender가 호출되고 내용에 비밀번호가 포함된다")
  void sendResetPasswordEmail_success() {
    // given
    String email = "test@example.com";
    String tempPassword = "temp1234!";

    // when
    mailService.sendResetPasswordEmail(email, tempPassword);

    // then
    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(asyncMailSender).send(eq(email), contentCaptor.capture());

    String content = contentCaptor.getValue();
    assertTrue(content.contains(tempPassword));
    assertTrue(content.contains("임시 비밀번호"));
  }

  @Test
  @DisplayName("임시 비밀번호 생성 시 10자리 랜덤 문자열이 반환된다")
  void generateTempPassword_success() {
    String tempPassword = mailService.generateTempPassword();

    assertNotNull(tempPassword);
    assertEquals(10, tempPassword.length());
  }
}
