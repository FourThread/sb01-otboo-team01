package com.fourthread.ozang.app.domain.feed.repository;

import com.fourthread.ozang.core.domain.feed.entity.Feed;
import com.fourthread.ozang.core.domain.feed.entity.FeedLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

  Boolean existsByFeed_IdAndUser_Id(UUID feedId, UUID userId);

  Optional<FeedLike> findByFeed_IdAndUser_Id(UUID feedId, UUID userId);

  void deleteAllByFeed_Id(UUID feedId);

  Object findByFeed(Feed feed);
}
