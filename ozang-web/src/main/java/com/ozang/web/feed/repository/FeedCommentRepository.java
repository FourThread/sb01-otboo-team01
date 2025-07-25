package com.ozang.web.feed.repository;

import com.ozang.web.feed.entity.FeedComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID>, FeedCommentRepositoryCustom {

  List<FeedComment> findByFeed_IdAndAuthor_Id(UUID feedId, UUID authorId);
}
