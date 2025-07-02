package com.fourthread.ozang.module.domain.clothes.repository.query;

import com.fourthread.ozang.module.config.QuerydslConfig;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortBy;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.user.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
@Import({QuerydslConfig.class})
@ActiveProfiles("test")
class ClothesAttributeDefinitionRepositoryImplTest {

    @Autowired
    private ClothesAttributeDefinitionRepositoryImpl repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        List<ClothesAttributeDefinition> definitions = List.of(
                new ClothesAttributeDefinition("색상", List.of("빨강", "파랑")),
                new ClothesAttributeDefinition("소재", List.of("면", "폴리")),
                new ClothesAttributeDefinition("핏", List.of("슬림", "오버"))
        );
        definitions.forEach(entityManager::persist);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAllByCondition_정렬조건없고_모두조회() {
        List<ClothesAttributeDefinition> results = repository.findAllByCondition(
                null, null, 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                null
        );

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getName()).isEqualTo("소재"); // 사전순 정렬
    }

    @Test
    void findAllByCondition_키워드로검색() {
        List<ClothesAttributeDefinition> results = repository.findAllByCondition(
                null, null, 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                "소"
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("소재");
    }

    @Test
    void findAllByCondition_커서기반_다음페이지조회() {
        ClothesAttributeDefinition 기준 = repository.findAllByCondition(
                null, null, 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                null
        ).get(0);

        List<ClothesAttributeDefinition> results = repository.findAllByCondition(
                기준.getName(), 기준.getId(), 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                null
        );

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ClothesAttributeDefinition::getName)
                .doesNotContain(기준.getName());
    }

    @Test
    void countByCondition_키워드기반_개수조회() {
        int count = repository.countByCondition("핏");
        assertThat(count).isEqualTo(1);
    }
}
