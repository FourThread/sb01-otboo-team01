package com.fourthread.ozang.module.domain.follow.controller;

import com.fourthread.ozang.module.domain.follow.dto.FollowCreateRequest;
import com.fourthread.ozang.module.domain.follow.dto.FollowDto;
import com.fourthread.ozang.module.domain.follow.dto.FollowSummaryDto;
import com.fourthread.ozang.module.domain.follow.service.FollowService;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> createFollow(
            @RequestBody FollowCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UUID followerId = userDetails.getUserDto().id(); // 로그인 사용자 기준
        FollowDto dto = followService.createFollow(followerId, request.getFolloweeId());
        return ResponseEntity.status(201).body(dto);
    }


    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> getFollowSummary(
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UUID currentUserId = userDetails.getUserDto().id();
        FollowSummaryDto summary = followService.getFollowSummary(userId, currentUserId);
        return ResponseEntity.ok(summary);
    }


    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> deleteFollow(
            @PathVariable UUID followId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        followService.deleteFollow(followId, userDetails.getUserDto().id());
        return ResponseEntity.noContent().build();
    }
}