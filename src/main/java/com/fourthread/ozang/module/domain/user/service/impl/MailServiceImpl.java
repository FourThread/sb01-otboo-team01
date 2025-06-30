package com.fourthread.ozang.module.domain.user.service.impl;

import com.fourthread.ozang.module.domain.user.service.MailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailServiceImpl implements MailService {

  @Transactional
  @Override
  public String sendResetPasswordEmail(String email) {
    return "";
  }

  @Override
  public void sendMail(String toMail, String title, String content) {

  }
}
