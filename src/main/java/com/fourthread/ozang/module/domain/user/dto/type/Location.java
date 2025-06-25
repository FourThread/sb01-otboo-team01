package com.fourthread.ozang.module.domain.user.dto.type;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

  private Double latitude;
  private Double longitude;
  private Integer x;
  private Integer y;

  @ElementCollection(fetch = FetchType.LAZY)
  private List<String> locationNames;

  public Location(Double latitude, Double longitude, Integer x, Integer y, List<String> locationNames) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.x = x;
    this.y = y;
    this.locationNames = locationNames;
  }
}

