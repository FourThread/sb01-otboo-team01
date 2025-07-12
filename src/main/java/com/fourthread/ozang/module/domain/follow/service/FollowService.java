package com.fourthread.ozang.module.domain.follow.service;


import com.fourthread.ozang.module.domain.follow.dto.FollowDto;
import com.fourthread.ozang.module.domain.follow.dto.FollowSummaryDto;
import com.fourthread.ozang.module.domain.follow.entity.Follow;
import com.fourthread.ozang.module.domain.follow.mapper.FollowMapper;
import com.fourthread.ozang.module.domain.follow.repository.FollowRepository;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;

    /**
     * 팔로우 생성
     */
    @Transactional
    public FollowDto createFollow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        boolean alreadyExists = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        if (alreadyExists) {
            throw new IllegalStateException("이미 팔로우 중입니다.");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("팔로워 사용자를 찾을 수 없습니다."));
        User followee = userRepository.findById(followeeId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 대상을 찾을 수 없습니다."));

        Follow follow = followRepository.save(new Follow(follower, followee));
        return followMapper.toDto(follow);
    }

    /**
     * 팔로우 요약 정보 조회
     */
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
    }

    /**
     * 팔로우 취소 (삭제)
     */
    @Transactional
    public void deleteFollow(UUID followId, UUID requesterId) {
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new IllegalArgumentException("팔로우 정보를 찾을 수 없습니다."));

        if (!follow.getFollower().getId().equals(requesterId)) {
            throw new SecurityException("본인의 팔로우만 취소할 수 있습니다.");
        }

        followRepository.deleteById(followId);
    }
}