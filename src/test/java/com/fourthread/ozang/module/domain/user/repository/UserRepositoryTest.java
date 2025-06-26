//package com.fourthread.ozang.module.domain.user.repository;
//
//import com.fourthread.ozang.module.config.AppConfig;
//import com.fourthread.ozang.module.config.QuerydslConfig;
//import com.fourthread.ozang.module.domain.user.dto.type.Role;
//import com.fourthread.ozang.module.domain.user.entity.User;
//import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
//import java.util.Optional;
//import static org.assertj.core.api.Assertions.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//
//@DataJpaTest
//@ActiveProfiles("test")
//@Import({AppConfig.class, QuerydslConfig.class, UserMapper.class})
//public class UserRepositoryTest {
//
//  @Autowired
//  private UserRepository userRepository;
//
//  private User savedUser;
//
//  @BeforeEach
//  void setUp() {
//    User user = new User("test", "test@example.com", "testPassword");
//    savedUser = userRepository.save(user);
//  }
//
//  @Test
//  @DisplayName("사용자 조회하기 ")
//  void findByUsername() {
//    Optional<User> findUser = userRepository.findById(savedUser.getId());
//
//    assertThat(findUser).isPresent();
//    assertThat(findUser.get().getId()).isEqualTo(savedUser.getId());
//  }
//
//  @Nested
//  @DisplayName("User 수정 테스트")
//  class UserUpdateTest {
//    @Test
//    @DisplayName("비밀번호 변경")
//    void updatePassword() {
//      savedUser.updatePassword("newPassword");
//
//      User updated = userRepository.findById(savedUser.getId()).orElseThrow();
//      assertThat(updated.getPassword()).isEqualTo("newPassword");
//    }
//
//    @Test
//    @DisplayName("권한 변경")
//    void updateRole() {
//      savedUser.updateRole(Role.ADMIN);
//
//      User updated = userRepository.findById(savedUser.getId()).orElseThrow();
//      assertThat(updated.getRole()).isEqualTo(Role.ADMIN);
//    }
//
//    @Test
//    @DisplayName("계정 잠금 상태 변경")
//    void updateLocked() {
//      savedUser.changeLocked(true);
//
//      User updated = userRepository.findById(savedUser.getId()).orElseThrow();
//      assertThat(updated.getLocked()).isEqualTo(true);
//    }
//  }
//}
