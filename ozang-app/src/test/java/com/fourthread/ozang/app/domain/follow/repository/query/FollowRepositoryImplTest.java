package com.fourthread.ozang.app.domain.follow.repository.query;

import com.fourthread.ozang.core.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.core.domain.follow.dto.FollowSummaryProjection;
import com.fourthread.ozang.core.domain.follow.entity.Follow;
import com.fourthread.ozang.core.domain.follow.repository.FollowRepositoryImpl;
import com.fourthread.ozang.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=test-admin",
        "ADMIN_EMAIL=test-admin@mail.com",
        "ADMIN_PASSWORD=test-pass",
        "JWT_SECRET=d12d12d21d21d12d2",
        "KAKAO_API_KEY=test",
        "WEATHER_API_KEY=dwqqdd11",
        "AWS_ACCESS_KEY=testAccessKey",
        "AWS_SECRET_KEY=testSecretKey",
        "cloud.aws.region.static=ap-northeast-2"
})
@ActiveProfiles("test")
class FollowRepositoryImplTest {

    @Autowired
    private FollowRepositoryImpl followRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = new User("user1", "user1@example.com", "pwd");
        user2 = new User("user2", "user2@example.com", "pwd");
        user3 = new User("user3", "user3@example.com", "pwd");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();

        userId1 = user1.getId();
        userId2 = user2.getId();
        userId3 = user3.getId();

        entityManager.persist(new Follow(user1, user2)); // user1 → user2
        entityManager.persist(new Follow(user1, user3)); // user1 → user3
        entityManager.persist(new Follow(user3, user1)); // user3 → user1
        entityManager.flush();
        entityManager.clear();
    }

    @DisplayName("팔로우 요약 정보를 조회할 수 있다")
    @Test
    void findFollowSummary_success() {
        FollowSummaryProjection summary = followRepository.findFollowSummary(user2.getId(), user1.getId());

        assertThat(summary.getFollowerCount()).isEqualTo(1); // user1 → user2
        assertThat(summary.getFollowingCount()).isEqualTo(0); // user2는 아무도 팔로우하지 않음
        assertThat(summary.getFollowedByMeId()).isNotNull(); // user1 → user2
        assertThat(summary.getFollowingMeId()).isNull(); // user2 → user1 없음
    }

    @DisplayName("팔로잉 목록을 커서 기반으로 조회할 수 있다")
    @Test
    void findAllFollowingsByCondition_success() {
        List<Follow> result = followRepository.findAllFollowingsByCondition(
                user1.getId(), null, null, 10, null, "createdAt", SortDirection.ASCENDING
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(f -> f.getFollowee().getName())
                .containsExactlyInAnyOrder("user2", "user3");
    }

    @DisplayName("팔로워 목록을 커서 기반으로 조회할 수 있다")
    @Test
    void findAllFollowersByCondition_success() {
        List<Follow> result = followRepository.findAllFollowersByCondition(
                user1.getId(), null, null, 10, null, "createdAt", SortDirection.ASCENDING
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFollower().getName()).isEqualTo("user3");
    }

    @DisplayName("팔로잉 수를 검색할 수 있다")
    @Test
    void countFollowings_success() {
        int count = followRepository.countFollowings(user1.getId(), null);
        assertThat(count).isEqualTo(2); // user1 → user2, user3
    }

    @DisplayName("팔로워 수를 검색할 수 있다")
    @Test
    void countFollowers_success() {
        int count = followRepository.countFollowers(user1.getId(), null);
        assertThat(count).isEqualTo(1); // user3 → user1
    }

    @DisplayName("팔로잉 검색에서 이름으로 필터링할 수 있다")
    @Test
    void findAllFollowingsByNameLike() {
        List<Follow> result = followRepository.findAllFollowingsByCondition(
                user1.getId(), null, null, 10, "3", "createdAt", SortDirection.ASCENDING
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFollowee().getName()).isEqualTo("user3");
    }

    @DisplayName("팔로워 검색에서 이름으로 필터링할 수 있다")
    @Test
    void findAllFollowersByNameLike() {
        List<Follow> result = followRepository.findAllFollowersByCondition(
                user1.getId(), null, null, 10, "3", "createdAt", SortDirection.ASCENDING
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFollower().getName()).isEqualTo("user3");
    }

}