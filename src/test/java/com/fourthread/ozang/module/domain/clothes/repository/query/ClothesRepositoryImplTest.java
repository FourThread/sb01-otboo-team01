package com.fourthread.ozang.module.domain.clothes.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import software.amazon.awssdk.services.s3.S3Client;


@Transactional
@SpringBootTest
@ActiveProfiles("test")
class ClothesRepositoryImplTest {

    @Autowired
    private ClothesRepositoryImpl repository;

    @Autowired
    private EntityManager em;

    UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        List<Clothes> clothesList = List.of(
                Clothes.builder()
                        .ownerId(ownerId)
                        .name("상의")
                        .type(ClothesType.TOP)
                        .build(),
                Clothes.builder()
                        .ownerId(ownerId)
                        .name("하의")
                        .type(ClothesType.BOTTOM)
                        .build(),
                Clothes.builder()
                        .ownerId(ownerId)
                        .name("신발")
                        .type(ClothesType.SHOES)
                        .build()
        );

        clothesList.forEach(em::persist);
        em.flush();
        em.clear();
    }

    @DisplayName("조건 없이 모든 옷을 조회할 수 있다.")
    @Test
    void findAllByCondition_all() {
        List<Clothes> result = repository.findAllByCondition(
                ownerId, null, null, 10,
                null, "CREATED_AT", SortDirection.ASCENDING
        );

        assertThat(result).hasSize(3);
    }

    @DisplayName("ClothesType 기준으로 필터링해서 조회할 수 있다.")
    @Test
    void findAllByCondition_withType() {
        List<Clothes> result = repository.findAllByCondition(
                ownerId, null, null, 10,
                ClothesType.TOP, "CREATED_AT", SortDirection.ASCENDING
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(ClothesType.TOP);
    }

    @DisplayName("커서 기반 다음 페이지 조회를 할 수 있다.")
    @Test
    void findAllByCondition_withCursor() {
        List<Clothes> clothes = repository.findAllByCondition(
                ownerId, null, null, 10,
                null, "CREATED_AT", SortDirection.ASCENDING
        );

        Clothes first = clothes.get(0);

        List<Clothes> next = repository.findAllByCondition(
                ownerId,
                first.getCreatedAt().toString(),
                first.getId(),
                10,
                null, "CREATED_AT", SortDirection.ASCENDING
        );

        assertThat(next).hasSize(2);
        assertThat(next).extracting(Clothes::getId).doesNotContain(first.getId());
    }

    @DisplayName("ownerId와 ClothesType으로 옷 개수를 조회할 수 있다.")
    @Test
    void countByOwnerAndType() {
        int count = repository.countByOwnerAndType(ownerId, ClothesType.BOTTOM);
        assertThat(count).isEqualTo(1);
    }
}