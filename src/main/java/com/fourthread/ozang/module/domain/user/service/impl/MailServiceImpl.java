package com.fourthread.ozang.module.domain.user.service.impl;

import com.fourthread.ozang.module.domain.user.service.MailService;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

  private final JavaMailSender mailSender;
  private static final String TITLE = "[O-ZANG] 비밀번호 재설정 안내";

  @Transactional
  @Override
  public String sendResetPasswordEmail(String email) {
    String tempPassword = generateTempPassword();
    String content = """
        안녕하세요, O-ZANG입니다.

        요청하신 임시 비밀번호는 아래와 같습니다:

        ▶ 임시 비밀번호: %s

        본 임시 비밀번호는 발급 시점부터 1시간 동안만 유효합니다.
        로그인 후 반드시 비밀번호를 변경해주세요.

        감사합니다.
        """.formatted(tempPassword);

    sendMail(email, TITLE, content);
    return tempPassword;
  }

  @Override
  public void sendMail(String toMail, String title, String content) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(toMail);
    message.setSubject(title);
    message.setText(content);
    mailSender.send(message);
  }

  // 10자리 난수 + 영문 조합 -> 임시 비밀번호 생성함
  private String generateTempPassword() {
    int length = 10;
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }
}
