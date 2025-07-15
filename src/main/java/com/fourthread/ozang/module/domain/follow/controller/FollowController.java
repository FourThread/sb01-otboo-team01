package com.fourthread.ozang.module.domain.follow.controller;

import com.fourthread.ozang.module.domain.follow.dto.FollowCreateRequest;
import com.fourthread.ozang.module.domain.follow.dto.FollowDto;
import com.fourthread.ozang.module.domain.follow.dto.FollowListResponse;
import com.fourthread.ozang.module.domain.follow.dto.FollowSummaryDto;
import com.fourthread.ozang.module.domain.follow.service.FollowService;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
}