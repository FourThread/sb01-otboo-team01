package com.fourthread.ozang.module.domain.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("인증된 사용자 없이 엔드포인트를 호출할 수 없습니다")
  void getProfile_unauthenticated() throws Exception {
    mvc.perform(get("/api/test/hello"))
        .andExpect(status().isUnauthorized());
  }
}
