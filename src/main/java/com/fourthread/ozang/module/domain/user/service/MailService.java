package com.fourthread.ozang.module.domain.user.service;

public interface MailService {

  String sendResetPasswordEmail(String email);

  void sendMail(String toMail, String title, String content);

}
