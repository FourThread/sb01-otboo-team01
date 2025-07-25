package com.ozang.web.feed.elasticsearch.repository;

import com.ozang.web.feed.elasticsearch.entity.FeedDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedElasticsearchRepository extends ElasticsearchRepository<FeedDocument, String> {

}
