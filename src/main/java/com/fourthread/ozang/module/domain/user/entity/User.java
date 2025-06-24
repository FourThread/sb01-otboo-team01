package com.fourthread.ozang.module.domain.user.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.user.dto.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

  @Column(length = 100, nullable = false, unique = true)
  private String name;
  @Column(length = 100, nullable = false, unique = true)
  private String email;
  @Column(length = 100, nullable = false)
  private String password;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;
  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private boolean locked;
//  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//  @JoinColumn(name = "profile_id", columnDefinition = "uuid")
//  private Profile profile;

  // 사용자 초기화(초기 Role은 User이고 계정 잠금은 False)
  public User(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = Role.USER;
    this.locked = false;
  }

  // 권한 수정
  public void updateRole(Role role) {
    if (this.role != role) {
      this.role = role;
    }
  }

  public void updatePassword(String password) {
    if (!this.password.equals(password)) {
      this.password = password;
    }
  }

  public void changeLocked(boolean locked) {
    if (this.locked != locked) {
      this.locked = locked;
    }
  }
}
