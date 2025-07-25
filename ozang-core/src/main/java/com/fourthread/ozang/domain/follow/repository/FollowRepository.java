package com.fourthread.ozang.domain.follow.repository;

import com.fourthread.ozang.domain.follow.entity.Follow;
import com.fourthread.ozang.domain.follow.repository.query.FollowRepositoryCustom;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowRepositoryCustom {

    // 팔로우 여부 확인 (중복 방지)
    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    // 팔로우 엔티티 조회 (follow 취소할 때 사용)
    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    // 팔로워 수 (내가 얼마나 팔로우당하고 있는가)
    long countByFolloweeId(UUID followeeId);

    // 팔로잉 수 (내가 얼마나 팔로잉하고 있는가)
    long countByFollowerId(UUID followerId);

    // 특정 사용자를 팔로우 중인 사람들
    @Query("select f.follower.id from Follow f where f.followee.id = :followeeId")
    Set<UUID> findFollowerIdsByFolloweeId(@Param("followeeId") UUID followeeId);

    // 특정 사용자가 팔로우한 사람들
    List<Follow> findAllByFollowerId(UUID followerId);
}