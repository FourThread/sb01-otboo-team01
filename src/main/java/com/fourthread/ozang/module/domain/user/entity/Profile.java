package com.fourthread.ozang.module.domain.user.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "profiles")
public class Profile extends BaseUpdatableEntity {

  @Column(length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Gender gender;

  private LocalDate birthDate;

  @Embedded
  @Column(length = 50)
  private Location location;

  private Integer temperatureSensitivity;

  @Column(length = 2048)
  private String profileImageUrl;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  private User user;

  public Profile(String name, Gender gender, LocalDate birthDate, Location location,
      Integer temperatureSensitivity, String profileImageUrl) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.location = location;
    this.temperatureSensitivity = temperatureSensitivity;
    this.profileImageUrl = profileImageUrl;
  }

  public void updateProfile(String name, Gender gender, LocalDate birthDate,
      Location location, Integer temperatureSensitivity, String profileImageUrl) {
    if (name != null) this.name = name;
    if (gender != null) this.gender = gender;
    if (birthDate != null) this.birthDate = birthDate;
    if (location != null) this.location = location;
    if (temperatureSensitivity != null) this.temperatureSensitivity = temperatureSensitivity;
    if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
