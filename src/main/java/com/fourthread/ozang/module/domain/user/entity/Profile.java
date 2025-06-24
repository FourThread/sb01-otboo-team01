package com.fourthread.ozang.module.domain.user.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.user.dto.Location;
import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseUpdatableEntity {

  @Column(length = 50, nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(length = 10, nullable = false)
  private Gender gender;

  @Column(nullable = false)
  private Instant birthDate;

  @Column(length = 50, nullable = false)
  private Location location;

  @Column(nullable = false)
  private int temperatureSensitivity;

  @Column(length = 2048)
  private String profileImageUrl;

  public Profile(String name, Gender gender, Instant birthDate, Location location,
      int temperatureSensitivity, String profileImageUrl) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.location = location;
    this.temperatureSensitivity = temperatureSensitivity;
    this.profileImageUrl = profileImageUrl;
  }

  public void updateProfile(String name, Gender gender, Instant birthDate,
      Location location, int temperatureSensitivity, String profileImageUrl) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.location = location;
    this.temperatureSensitivity = temperatureSensitivity;
    this.profileImageUrl = profileImageUrl;
  }
}
