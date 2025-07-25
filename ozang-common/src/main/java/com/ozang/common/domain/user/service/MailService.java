package com.fourthread.ozang.module.domain.user.service;

public interface MailService {

  void sendResetPasswordEmail(String email, String tempPassword);

  String generateTempPassword();

}
