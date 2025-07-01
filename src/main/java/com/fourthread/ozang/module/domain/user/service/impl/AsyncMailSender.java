package com.fourthread.ozang.module.domain.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncMailSender {

  private final JavaMailSender mailSender;
  private static final String TITLE = "[O-ZANG] 비밀번호 재설정 안내";
  private static final int RETRY_MAX_ATTEMPTS = 3;

  @Async
  @Retryable(
      retryFor = MailException.class,
      maxAttempts = RETRY_MAX_ATTEMPTS,
      backoff = @Backoff(delay = 1000)
  )
  public void send(String toMail, String content) {
    RetryContext context = RetrySynchronizationManager.getContext();
    int retryCount = context != null ? context.getRetryCount() : 0;
    log.info("[AsyncMailSender] 메일 전송: {}, 시도: {}/{}", toMail, retryCount + 1, RETRY_MAX_ATTEMPTS);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(toMail);
    message.setSubject(TITLE);
    message.setText(content);
    mailSender.send(message);
  }

  @Recover
  public void recover(MailException ex, String toMail, String content) {
    log.error("[AsyncMailSender] 메일 전송 최종 실패 - 대상: {}, 사유: {}", toMail, ex.getMessage(), ex);
  }
}
