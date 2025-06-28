package com.fourthread.ozang.module.domain.security;

import com.fourthread.ozang.module.domain.security.Service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdminInit implements ApplicationRunner {

  private final AuthService authService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.info("초기 실행 시 어드민 유저를 생성합니다!");
    authService.initAdmin();
  }
}
