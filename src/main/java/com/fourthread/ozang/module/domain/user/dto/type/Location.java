package com.fourthread.ozang.module.domain.user.dto.type;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

  private double latitude;
  private double longitude;
  private int x;
  private int y;

  @ElementCollection(fetch = FetchType.LAZY)
  private List<String> locationNames;

  public Location(double latitude, double longitude, int x, int y, List<String> locationNames) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.x = x;
    this.y = y;
    this.locationNames = locationNames;
  }
}

