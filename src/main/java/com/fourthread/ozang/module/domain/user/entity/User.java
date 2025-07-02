package com.fourthread.ozang.module.domain.user.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.user.dto.type.Items;
import com.fourthread.ozang.module.domain.user.dto.type.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

  @Column(length = 100, nullable = false, unique = true)
  private String name;
  @Column(length = 100, nullable = false, unique = true)
  private String email;
  @Setter
  @Column(length = 100, nullable = false)
  private String password;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;
  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private Boolean locked;
  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private Profile profile;
  @Setter @Getter
  private LocalDateTime tempPasswordIssuedAt;

  @ElementCollection(fetch = FetchType.LAZY)
  @BatchSize(size = 50)
  @Enumerated(EnumType.STRING)
  @CollectionTable(name = "user_oauth_providers", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "provider", length = 20)
  private List<Items> linkedOAuthProviders = new ArrayList<>();

  // 사용자 초기화(초기 Role은 User이고 계정 잠금은 False)
  public User(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = Role.USER;
    this.locked = false;
  }

  public User(String name, String email, String password, Role role) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
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

  public void changeLocked(Boolean locked) {
    if (this.locked != locked) {
      this.locked = locked;
    }
  }

  public void setProfile(Profile profile) {
    this.profile = profile;
    if (profile != null) {
      profile.setUser(this);
    }
  }

  // OAuth 제공자 추가
  public void addOAuthProvider(Items provider) {
    if (!this.linkedOAuthProviders.contains(provider)) {
      this.linkedOAuthProviders.add(provider);
    }
  }
}
