package com.fourthread.ozang.module.domain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.module.domain.user.dto.request.UserLockUpdateRequest;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private UserRepository userRepository;
  @MockitoBean
  private AuthenticationManager authenticationManager;

  @Test
  @DisplayName("[인증 실페] 인증된 사용자 없이 엔드포인트를 호출할 수 없습니다 - 401 Unauthorized 에러 발생")
  void getHelloWorld_unauthenticated() throws Exception {
    mvc.perform(get("/api/test/hello"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("[인증 성공] 모의 인증된 사용자 인증 시 엔드포인트 호출이 가능합니다.")
  void getHelloWorld_authenticated() throws Exception {
    mvc.perform(get("/api/test/hello"))
        .andExpect(content().string("Hello World!"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  @DisplayName("[인가 성공] ADMIN 권한은 계정 잠금 상태를 변경할 수 있다.")
  void changeLock_withAdminRole_success() throws Exception {
    User user = new User("test", "test@test.com", "Testtest1234!!", Role.USER);
    userRepository.save(user);

    UUID userId = user.getId();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    mvc.perform(patch("/api/users/{userId}/lock", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    User updatedUser = userRepository.findById(userId).orElseThrow();
    assertThat(updatedUser.getLocked()).isTrue();
  }

  @Test
  @WithMockUser(username = "user", roles = "USER")
  @DisplayName("[인가 실패] USER 권한으로 계정 잠금 상태 변경 시 403 Forbidden이 발생한다.")
  void changeLock_withUserRole_forbidden() throws Exception {
    User user = new User("test", "test@test.com", "Testtest1234!!", Role.USER);
    userRepository.save(user);

    UUID userId = user.getId();
    UserLockUpdateRequest request = new UserLockUpdateRequest(true);

    mvc.perform(patch("/api/users/{userId}/lock", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    User updatedUser = userRepository.findById(userId).orElseThrow();
    assertThat(updatedUser.getLocked()).isFalse();
  }
}
