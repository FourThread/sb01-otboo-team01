package com.fourthread.ozang.app.domain.user.service;

public interface MailService {

  void sendResetPasswordEmail(String email, String tempPassword);

  String generateTempPassword();

}
