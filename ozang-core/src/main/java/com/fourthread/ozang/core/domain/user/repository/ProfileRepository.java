package com.fourthread.ozang.core.domain.user.repository;

import com.fourthread.ozang.core.domain.user.entity.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  Optional<Profile> findByUserId(UUID userId);

  /**
   * 특정 격자 좌표에 위치한 사용자들의 프로필 조회
   * @param gridX 격자 X 좌표
   * @param gridY 격자 Y 좌표
   * @return 해당 격자에 위치한 사용자들의 프로필 목록
   */
  @Query("SELECT p FROM Profile p WHERE p.location.x = :gridX AND p.location.y = :gridY")
  List<Profile> findByGridCoordinates(@Param("gridX") Integer gridX, @Param("gridY") Integer gridY);

  /**
   * 특정 격자 좌표에 위치한 사용자 ID들만 조회
   * @param gridX 격자 X 좌표
   * @param gridY 격자 Y 좌표
   * @return 해당 격자에 위치한 사용자 ID 목록
   */
  @Query("SELECT p.user.id FROM Profile p WHERE p.location.x = :gridX AND p.location.y = :gridY")
  List<UUID> findUserIdsByGridCoordinates(@Param("gridX") Integer gridX, @Param("gridY") Integer gridY);

  /**
   * 격자 좌표가 설정된 모든 프로필 조회 (활성 지역 초기화용)
   * @return 위치 정보가 있는 프로필 목록
   */
  @Query("SELECT p FROM Profile p WHERE p.location.x IS NOT NULL AND p.location.y IS NOT NULL")
  List<Profile> findAllWithGridCoordinates();
}
