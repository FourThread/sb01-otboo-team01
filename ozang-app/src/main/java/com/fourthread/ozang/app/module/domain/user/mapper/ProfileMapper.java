package com.fourthread.ozang.module.domain.user.mapper;
import com.fourthread.ozang.module.domain.user.dto.data.LocationDto;
import com.fourthread.ozang.module.domain.user.dto.data.ProfileDto;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

  public ProfileDto toDto(Profile profile) {
    return new ProfileDto(
        profile.getUser().getId(),
        profile.getName(),
        profile.getGender(),
        profile.getBirthDate(),
        toLocationDto(profile.getLocation()),
        profile.getTemperatureSensitivity(),
        profile.getProfileImageUrl()
    );
  }

  public LocationDto toLocationDto(Location location) {
    if (location == null) return null;

    return new LocationDto(
        location.getLatitude(),
        location.getLongitude(),
        location.getX(),
        location.getY(),
        location.getLocationNamesList()
    );
  }
}
