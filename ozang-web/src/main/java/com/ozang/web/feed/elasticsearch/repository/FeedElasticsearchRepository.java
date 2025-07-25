package com.fourthread.ozang.module.domain.feed.elasticsearch.repository;

import com.fourthread.ozang.module.domain.feed.elasticsearch.entity.FeedDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedElasticsearchRepository extends ElasticsearchRepository<FeedDocument, String> {

}
