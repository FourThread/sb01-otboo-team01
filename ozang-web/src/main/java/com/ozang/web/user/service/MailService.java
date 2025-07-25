package com.ozang.web.user.service;

public interface MailService {

  void sendResetPasswordEmail(String email, String tempPassword);

  String generateTempPassword();

}
