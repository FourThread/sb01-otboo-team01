package com.fourthread.ozang.module.domain.clothes.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortBy;
import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest(properties = {
        "KAKAO_API_KEY=kakao-test",
        "WEATHER_API_KEY=api-test",
        "ADMIN_USERNAME=admin",
        "ADMIN_EMAIL=admin@mail.com",
        "ADMIN_PASSWORD=1234",
        "JWT_SECRET=1dfadfafafvdfa"

})
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
                new ClothesAttributeDefinition("핏", List.of("면", "폴리")),
                new ClothesAttributeDefinition("소재", List.of("슬림", "오버"))
        );
        definitions.forEach(entityManager::persist);
        entityManager.flush();
        entityManager.clear();
    }

    @DisplayName("의상 목록 조회를 할 수 있다.")
    @Test
    void findAll() {
        List<ClothesAttributeDefinition> results = repository.findAllByCondition(
                null, null, 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                null
        );

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getName()).isEqualTo("색상");
        assertThat(results)
                .extracting(ClothesAttributeDefinition::getName)
                .containsExactly("색상", "소재", "핏"); // 사전순 정렬
    }

    @DisplayName("의상 목록 조회를 키워드 검색을 통해 할 수 있다.")
    @Test
    void findAllByCondition_shouldReturnResultsFilteredByKeyword() {
        List<ClothesAttributeDefinition> results = repository.findAllByCondition(
                null, null, 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                "소"
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("소재");
    }

    @DisplayName("커서 기반 정렬 후 다음 페이지를 조회할 수 있다")
    @Test
    void findAllByCondition_shouldReturnNextPageUsingCursor() {
        ClothesAttributeDefinition definition = repository.findAllByCondition(
                null, null, 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                null
        ).get(0);

        List<ClothesAttributeDefinition> results = repository.findAllByCondition(
                definition.getName(), definition.getId(), 10,
                SortBy.NAME,
                SortDirection.ASCENDING,
                null
        );

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ClothesAttributeDefinition::getName)
                .doesNotContain(definition.getName());
    }

    @DisplayName("키워드 조건으로 의상 속성 정의 개수를 조회할 수 있다")
    @Test
    void countByCondition_shouldReturnCountFilteredByKeyword() {
        int count = repository.countByCondition("핏");
        assertThat(count).isEqualTo(1);
    }
}
