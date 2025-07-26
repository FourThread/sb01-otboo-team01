package com.fourthread.ozang.app.domain.feed.repository;

import com.fourthread.ozang.core.domain.feed.entity.Feed;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

  Optional<Feed> findByAuthor_Id(UUID authorId);
}
