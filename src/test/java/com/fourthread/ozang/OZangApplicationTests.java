package com.fourthread.ozang;

import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OZangApplicationTests {

    @MockitoBean
    private FeedElasticsearchRepository feedElasticsearchRepository;

    @MockitoBean
    private FeedSearchService feedSearchService;

    @Test
    void contextLoads() {
    }

}
