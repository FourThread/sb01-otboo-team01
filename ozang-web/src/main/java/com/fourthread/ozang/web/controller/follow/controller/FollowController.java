package com.fourthread.ozang.domain.follow.controller;

import com.fourthread.ozang.domain.follow.dto.FollowCreateRequest;
import com.fourthread.ozang.domain.follow.dto.FollowDto;
import com.fourthread.ozang.domain.follow.dto.FollowListResponse;
import com.fourthread.ozang.domain.follow.dto.FollowSummaryDto;
import com.fourthread.ozang.domain.follow.service.FollowService;
import com.fourthread.ozang.domain.security.userdetails.UserDetailsImpl;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> createFollow(
            @RequestBody @Validated FollowCreateRequest request
    ) {
        FollowDto dto = followService.createFollow(request.followerId(), request.followeeId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> getFollowSummary(
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UUID requesterId = userDetails.getPayloadDto().userId();

        FollowSummaryDto summary = followService.getFollowSummary(userId, requesterId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(summary);
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> deleteFollow(
            @PathVariable UUID followId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UUID requesterId = userDetails.getPayloadDto().userId();
        followService.deleteFollow(followId, requesterId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping("/followings")
    public ResponseEntity<FollowListResponse> findFollowings(
            @RequestParam UUID followerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String nameLike,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESCENDING") String sortDirection
    ) {
        FollowListResponse response = followService.findAllFollowings(
                followerId, cursor, idAfter, limit, nameLike, sortBy, sortDirection
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers")
    public ResponseEntity<FollowListResponse> findFollowers(
            @RequestParam UUID followeeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String nameLike,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESCENDING") String sortDirection
    ) {
        FollowListResponse response = followService.findAllFollowers(
                followeeId, cursor, idAfter, limit, nameLike, sortBy, sortDirection
        );
        return ResponseEntity.ok(response);
    }
}