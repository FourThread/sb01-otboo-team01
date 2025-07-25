package com.fourthread.ozang.domain.feed.repository;

import com.fourthread.ozang.domain.feed.entity.Feed;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

  Optional<Feed> findByAuthor_Id(UUID authorId);
}
