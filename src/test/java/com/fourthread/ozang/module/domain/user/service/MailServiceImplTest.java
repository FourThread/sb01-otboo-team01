package com.fourthread.ozang.module.domain.user.service;

import com.fourthread.ozang.module.domain.user.service.impl.MailServiceImpl;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import static org.mockito.Mockito.any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class MailServiceImplTest {

  private JavaMailSender mailSender;
  private MailServiceImpl mailService;

  @BeforeEach
  void setUp() {
    mailSender = mock(JavaMailSender.class);
    mailService = new MailServiceImpl(mailSender);
  }

  @Test
  @DisplayName("정상적으로_임시비밀번호_생성하고_메일_전송한다")
  void sendResetPasswordEmail_success() {
    String email = "test@example.com";

    String tempPassword = mailService.sendResetPasswordEmail(email);

    assertThat(tempPassword).isNotNull();
    assertThat(tempPassword.length()).isEqualTo(10);

    verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
  }
}
