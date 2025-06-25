package com.fourthread.ozang.module.domain.feed.repository;

import com.fourthread.ozang.module.domain.feed.entity.FeedLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

  Boolean existsByFeed_IdAndUser_Id(UUID feedId, UUID userId);
}
