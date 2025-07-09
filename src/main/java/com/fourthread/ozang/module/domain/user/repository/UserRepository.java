package com.fourthread.ozang.module.domain.user.repository;

import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.custom.UserCustomRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, UserCustomRepository {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByName(String username);

  Optional<User> findByName(String name);

  /**
   * =============== 새로운 메서드 추가 ===============
   * 위치 정보로 사용자 조회 (날씨 알림용)
   */
  @Query("SELECT u FROM User u " +
      "JOIN u.profile p " +
      "WHERE p.location.locationNames LIKE %:location%")
  List<User> findByProfileLocationContaining(@Param("location") String location);

  /**
   * =============== 새로운 메서드 추가 ===============
   * 날씨 알림 활성화된 사용자 조회
   */
  @Query("SELECT u FROM User u " +
      "WHERE u.weatherAlertEnabled = true " +
      "AND u.profile.location.locationNames LIKE %:location%")
  List<User> findActiveUsersByLocation(@Param("location") String location);

  /**
   * =============== 새로운 메서드 추가 ===============
   * 온도 민감도별 사용자 조회
   */
  @Query("SELECT u FROM User u " +
      "JOIN u.profile p " +
      "WHERE p.temperatureSensitivity = :sensitivity")
  List<User> findByTemperatureSensitivity(@Param("sensitivity") Integer sensitivity);
}
