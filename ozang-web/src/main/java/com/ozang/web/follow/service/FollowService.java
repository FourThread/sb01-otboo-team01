package com.ozang.web.follow.service;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.ozang.web.clothes.dto.response.SortDirection;
import com.ozang.web.follow.dto.FollowDto;
import com.ozang.web.follow.dto.FollowListResponse;
import com.ozang.web.follow.dto.FollowSummaryDto;
import com.ozang.web.follow.dto.FollowSummaryProjection;
import com.ozang.web.follow.entity.Follow;
import com.ozang.web.follow.exception.FollowsException;
import com.ozang.web.follow.mapper.FollowMapper;
import com.ozang.web.follow.repository.FollowRepository;
import com.ozang.web.notification.event.ClothesAttributeAddedEvent;
import com.ozang.web.notification.event.FollowedEvent;
import com.ozang.web.user.entity.User;
import com.ozang.web.user.exception.UserException;
import com.ozang.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fourthread.ozang.module.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FollowDto createFollow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new FollowsException(SELF_FOLLOW_NOT_ALLOWED, this.getClass().getSimpleName(), SELF_FOLLOW_NOT_ALLOWED.getMessage());
        }

        boolean alreadyExists = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        if (alreadyExists) {
            throw new FollowsException(ALREADY_FOLLOWING, this.getClass().getSimpleName(), ALREADY_FOLLOWING.getMessage());
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND, this.getClass().getSimpleName(), USER_NOT_FOUND.getMessage()));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND, this.getClass().getSimpleName(), USER_NOT_FOUND.getMessage()));

        Follow follow = followRepository.save(new Follow(follower, followee));

        FollowDto dto = followMapper.toDto(follow);
        eventPublisher.publishEvent(new FollowedEvent(dto));

        return dto;
    }

    @Transactional(readOnly = true)
    public FollowSummaryDto getFollowSummary(UUID targetUserId, UUID currentUserId) {
        long followerCount = followRepository.countByFolloweeId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        Optional<Follow> followedByMe = followRepository.findByFollowerIdAndFolloweeId(currentUserId, targetUserId);
        Optional<Follow> followingMe = followRepository.findByFollowerIdAndFolloweeId(targetUserId, currentUserId);

        return new FollowSummaryDto(
                targetUserId,
                followerCount,
                followingCount,
                followedByMe.isPresent(),
                followedByMe.map(Follow::getId).orElse(null),
                followingMe.isPresent()
        );

//        FollowSummaryProjection projection = followRepository.findFollowSummary(targetUserId, currentUserId);
//        return new FollowSummaryDto(
//                targetUserId,
//                projection.getFollowerCount(),
//                projection.getFollowingCount(),
//                projection.isFollowedByMe(),
//                projection.getFollowedByMeId(),
//                projection.isFollowingMe()
//        );
    }

    @Transactional
    public void deleteFollow(UUID followId, UUID requesterId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new FollowsException(FOLLOWS_NOT_FOUND, this.getClass().getSimpleName(), FOLLOWS_NOT_FOUND.getMessage()));

        if (!follow.getFollower().getId().equals(requesterId)) {
            throw new SecurityException("본인의 팔로우만 취소할 수 있습니다.");
        }

        followRepository.deleteById(followId);
    }

    @Transactional(readOnly = true)
    public FollowListResponse findAllFollowings(UUID followerId,
                                                String cursor,
                                                UUID idAfter,
                                                int limit,
                                                String nameLike,
                                                String sortBy,
                                                String sortDirection) {

        validateSortBy(sortBy);
        SortDirection direction = SortDirection.from(sortDirection);

        List<Follow> results = followRepository.findAllFollowingsByCondition(
                followerId, cursor, idAfter, limit + 1, nameLike, sortBy, direction
        );

        boolean hasNext = results.size() > limit;
        List<Follow> pageContent = hasNext ? results.subList(0, limit) : results;

        UUID nextId = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;
        String nextCursor = hasNext ? getCursorValue(pageContent.get(pageContent.size() - 1), sortBy) : null;

        int totalCount = followRepository.countFollowings(followerId, nameLike);

        List<FollowDto> dtoList = pageContent.stream()
                .map(followMapper::toDto)
                .toList();

        return new FollowListResponse(
                dtoList,
                nextCursor,
                nextId,
                hasNext,
                totalCount,
                sortBy.toUpperCase(),
                direction.name()
        );
    }


    @Transactional(readOnly = true)
    public FollowListResponse findAllFollowers(UUID followeeId,
                                                    String cursor,
                                                    UUID idAfter,
                                                    int limit,
                                                    String nameLike,
                                                    String sortBy,
                                                    String sortDirection) {
        validateSortBy(sortBy);
        SortDirection direction = SortDirection.from(sortDirection);

        List<Follow> results = followRepository.findAllFollowersByCondition(
                followeeId, cursor, idAfter, limit + 1, nameLike, sortBy, direction
        );

        boolean hasNext = results.size() > limit;
        List<Follow> pageContent = hasNext ? results.subList(0, limit) : results;

        UUID nextId = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;
        String nextCursor = hasNext ? getCursorValue(pageContent.get(pageContent.size() - 1), sortBy) : null;

        int totalCount = followRepository.countFollowers(followeeId, nameLike);

        List<FollowDto> dtoList = pageContent.stream()
                .map(followMapper::toDto)
                .toList();

        return new FollowListResponse(
                dtoList,
                nextCursor,
                nextId,
                hasNext,
                totalCount,
                sortBy.toUpperCase(),
                direction.name()
        );
    }

    private void validateSortBy(String sortBy) {
        if (!"createdAt".equalsIgnoreCase(sortBy)) {
            throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + sortBy);
        }
    }

    private String getCursorValue(Follow follow, String sortBy) {
        if ("createdAt".equalsIgnoreCase(sortBy)) {
            return follow.getCreatedAt().toString();
        }

        throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + sortBy);
    }

}