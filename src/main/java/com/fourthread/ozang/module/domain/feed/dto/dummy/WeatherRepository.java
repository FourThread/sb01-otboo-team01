package com.fourthread.ozang.module.domain.feed.dto.dummy;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

}
