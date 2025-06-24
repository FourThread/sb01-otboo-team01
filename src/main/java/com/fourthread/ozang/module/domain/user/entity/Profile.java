package com.fourthread.ozang.module.domain.user.entity;

import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseUpdatableEntity {

  @Column(length = 50)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Gender gender;

  private LocalDateTime birthDate;

  @Embedded
  @Column(length = 50)
  private Location location;

  private Integer temperatureSensitivity;

  @Column(length = 2048)
  private String profileImageUrl;

  public Profile(String name, Gender gender, LocalDateTime birthDate, Location location,
      Integer temperatureSensitivity, String profileImageUrl) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.location = location;
    this.temperatureSensitivity = temperatureSensitivity;
    this.profileImageUrl = profileImageUrl;
  }

  public void updateProfile(String name, Gender gender, LocalDateTime birthDate,
      Location location, Integer temperatureSensitivity, String profileImageUrl) {
    this.name = name;
    this.gender = gender;
    this.birthDate = birthDate;
    this.location = location;
    this.temperatureSensitivity = temperatureSensitivity;
    this.profileImageUrl = profileImageUrl;
  }
}
