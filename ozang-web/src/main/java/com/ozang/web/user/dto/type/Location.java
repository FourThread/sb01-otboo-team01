package com.fourthread.ozang.module.domain.user.dto.type;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

  private Double latitude;
  private Double longitude;
  private Integer x;
  private Integer y;


  @Column(length = 500)
  private String locationNames;

  public Location(Double latitude, Double longitude, Integer x, Integer y, List<String> locationNames) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.x = x;
    this.y = y;
    this.locationNames = locationNames != null && !locationNames.isEmpty()
        ? String.join(",", locationNames)
        : null;
  }

  public List<String> getLocationNamesList() {
    if (locationNames == null || locationNames.trim().isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.asList(locationNames.split(","));
  }

  public void setLocationNames(List<String> locationNamesList) {
    this.locationNames = locationNamesList != null && !locationNamesList.isEmpty()
        ? String.join(",", locationNamesList)
        : null;
  }
}

