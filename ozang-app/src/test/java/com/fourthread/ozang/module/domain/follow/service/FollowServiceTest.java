package com.fourthread.ozang.module.domain.follow.service;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.follow.dto.FollowDto;
import com.fourthread.ozang.module.domain.follow.dto.FollowListResponse;
import com.fourthread.ozang.module.domain.follow.dto.FollowSummaryDto;
import com.fourthread.ozang.module.domain.follow.entity.Follow;
import com.fourthread.ozang.module.domain.follow.exception.FollowsException;
import com.fourthread.ozang.module.domain.follow.mapper.FollowMapper;
import com.fourthread.ozang.module.domain.follow.repository.FollowRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowMapper followMapper;

    @InjectMocks
    private FollowService followService;

    private UUID followerId;
    private UUID followeeId;
    private User follower;
    private User followee;
    private Follow follow;
    private FollowDto followDto;

    @BeforeEach
    void setUp() {
        followerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();

        follower = new User("follower", "follower@email.com", "pwd");
        followee = new User("followee", "followee@email.com", "pwd");

        ReflectionTestUtils.setField(follower, "id", followerId);
        ReflectionTestUtils.setField(followee, "id", followeeId);

        follow = new Follow(follower, followee);
        ReflectionTestUtils.setField(follow, "id", UUID.randomUUID());

        followDto = new FollowDto(follow.getId(), new UserSummary(followerId, "follower", null), new UserSummary(followeeId, "followee", null));
    }

    @DisplayName("팔로우를 성공적으로 생성할 수 있다")
    @Test
    void createFollow_success() {
        //given
        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).willReturn(false);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
        given(followRepository.save(any())).willReturn(follow);
        given(followMapper.toDto(any())).willReturn(followDto);

        //when
        FollowDto result = followService.createFollow(followerId, followeeId);

        //then
        assertThat(result).isEqualTo(followDto);
        then(followRepository).should().save(any(Follow.class));
    }

    @DisplayName("자기 자신을 팔로우하면 예외가 발생한다")
    @Test
    void createFollow_fail_selfFollow() {
        assertThatThrownBy(() -> followService.createFollow(followerId, followerId))
                .isInstanceOf(FollowsException.class);
    }

    @DisplayName("이미 팔로우한 경우 예외가 발생한다")
    @Test
    void createFollow_fail_alreadyExists() {
        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).willReturn(true);

        assertThatThrownBy(() -> followService.createFollow(followerId, followeeId))
                .isInstanceOf(FollowsException.class);
    }

    @DisplayName("팔로우 취소 성공")
    @Test
    void deleteFollow_success() {
        //given
        UUID followId = follow.getId();
        given(followRepository.findById(followId)).willReturn(Optional.of(follow));

        //when
        followService.deleteFollow(followId, followerId);

        //then
        then(followRepository).should().deleteById(followId);
    }

    @DisplayName("팔로우 취소 시 본인이 아닌 경우 예외 발생")
    @Test
    void deleteFollow_fail_unauthorized() {
        UUID followId = follow.getId();
        UUID otherUserId = UUID.randomUUID();
        given(followRepository.findById(followId)).willReturn(Optional.of(follow));

        assertThatThrownBy(() -> followService.deleteFollow(followId, otherUserId))
                .isInstanceOf(SecurityException.class);
    }

    @DisplayName("팔로우 삭제 시 followId가 존재하지 않으면 예외")
    @Test
    void deleteFollow_fail_not_found() {
        UUID followId = UUID.randomUUID();
        given(followRepository.findById(followId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.deleteFollow(followId, followerId))
                .isInstanceOf(FollowsException.class);
    }

    @DisplayName("팔로우 요약 정보를 정상적으로 조회할 수 있다")
    @Test
    void getFollowSummary_success() {
        UUID currentUserId = UUID.randomUUID();

        given(followRepository.countByFolloweeId(followeeId)).willReturn(3L);
        given(followRepository.countByFollowerId(followeeId)).willReturn(1L);
        given(followRepository.findByFollowerIdAndFolloweeId(currentUserId, followeeId)).willReturn(Optional.of(follow));
        given(followRepository.findByFollowerIdAndFolloweeId(followeeId, currentUserId)).willReturn(Optional.empty());

        FollowSummaryDto summary = followService.getFollowSummary(followeeId, currentUserId);

        assertThat(summary.followeeId()).isEqualTo(followeeId);
        assertThat(summary.followerCount()).isEqualTo(3L);
        assertThat(summary.followingCount()).isEqualTo(1L);
        assertThat(summary.followedByMe()).isTrue();
        assertThat(summary.followedByMeId()).isEqualTo(follow.getId());
        assertThat(summary.followingMe()).isFalse();
    }

    @DisplayName("팔로우한 사용자 목록을 커서 기반으로 조회할 수 있다")
    @Test
    void findAllFollowings_should_return_cursor_response() {
        // given
        String cursor = "2025-07-01T10:00:00";
        UUID idAfter = null;
        int limit = 10;
        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";

        Follow anotherFollow = new Follow(follower, followee);
        ReflectionTestUtils.setField(anotherFollow, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(anotherFollow, "createdAt", LocalDateTime.parse("2025-07-01T09:00:00"));

        List<Follow> mockResult = List.of(anotherFollow);

        given(followRepository.findAllFollowingsByCondition(followerId, cursor, idAfter, limit + 1, "", sortBy, SortDirection.DESCENDING))
                .willReturn(mockResult);

        given(followRepository.countFollowings(followerId, "")).willReturn(1);
        given(followMapper.toDto(any())).willReturn(followDto);

        // when
        FollowListResponse response = followService.findAllFollowings(followerId, cursor, idAfter, limit, "", sortBy, sortDirection);

        // then
        assertThat(response.data()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.sortBy()).isEqualTo("CREATEDAT");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");
    }

    @DisplayName("팔로워 목록을 커서 기반으로 조회할 수 있다")
    @Test
    void findAllFollowers_should_return_cursor_response() {
        // given
        String cursor = "2025-07-01T10:00:00";
        UUID idAfter = null;
        int limit = 10;
        String sortBy = "createdAt";
        String sortDirection = "DESCENDING";

        Follow anotherFollow = new Follow(follower, followee);
        ReflectionTestUtils.setField(anotherFollow, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(anotherFollow, "createdAt", LocalDateTime.parse("2025-07-01T09:00:00"));

        List<Follow> mockResult = List.of(anotherFollow);

        given(followRepository.findAllFollowersByCondition(followeeId, cursor, idAfter, limit + 1, "", sortBy, SortDirection.DESCENDING))
                .willReturn(mockResult);

        given(followRepository.countFollowers(followeeId, "")).willReturn(1);
        given(followMapper.toDto(any())).willReturn(followDto);

        //when
        FollowListResponse response = followService.findAllFollowers(followeeId, cursor, idAfter, limit, "", sortBy, sortDirection);

        //then
        assertThat(response.data()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.sortBy()).isEqualTo("CREATEDAT");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");
    }

    @DisplayName("지원하지 않는 정렬 필드로 조회 시 예외가 발생한다 - followings")
    @Test
    void findAllFollowings_should_throw_exception_when_sort_by_invalid() {
        //given
        String invalidSortBy = "INVALID";
        String sortDirection = "ASCENDING";

        //when then
        assertThatThrownBy(() -> followService.findAllFollowings(followerId, null, null, 10, "", invalidSortBy, sortDirection))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("지원하지 않는 정렬 필드로 조회 시 예외가 발생한다 - followers")
    @Test
    void findAllFollowers_should_throw_exception_when_sort_by_invalid() {
        //given
        String invalidSortBy = "INVALID";
        String sortDirection = "ASCENDING";

        //when then
        assertThatThrownBy(() -> followService.findAllFollowers(followeeId, null, null, 10, "", invalidSortBy, sortDirection))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("getCursorValue는 createdAt 기준으로 커서 문자열을 반환한다")
    @Test
    void getCursorValue_should_return_createdAt_string() {
        // given
        Follow follow = new Follow(follower, followee);
        LocalDateTime createdAt = LocalDateTime.of(2025, 7, 15, 12, 0);
        ReflectionTestUtils.setField(follow, "createdAt", createdAt);

        // when
        String cursor = ReflectionTestUtils.invokeMethod(followService, "getCursorValue", follow, "createdAt");

        // then
        assertThat(cursor).isEqualTo(createdAt.toString());
    }

    @DisplayName("getCursorValue는 지원하지 않는 정렬 기준일 경우 예외를 발생시킨다")
    @Test
    void getCursorValue_should_throw_when_sortBy_invalid() {
        Follow follow = new Follow(follower, followee);

        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(followService, "getCursorValue", follow, "invalidField")
        )
                .isInstanceOf(IllegalArgumentException.class);
    }
}
