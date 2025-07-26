package com.fourthread.ozang.app.domain.feed.repository;

import com.fourthread.ozang.core.domain.feed.entity.FeedComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID>, FeedCommentRepositoryCustom {

  List<FeedComment> findByFeed_IdAndAuthor_Id(UUID feedId, UUID authorId);
}
