package com.fourthread.ozang.core.domain.follow.entity;

import com.fourthread.ozang.core.domain.BaseEntity;
import com.fourthread.ozang.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"}), schema = "public")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    public Follow(User follower, User followee) {
        if (follower == null || followee == null) {
            throw new IllegalArgumentException("팔로워와 팔로우 대상은 null일 수 없습니다.");
        }
        this.follower = follower;
        this.followee = followee;
    }

}