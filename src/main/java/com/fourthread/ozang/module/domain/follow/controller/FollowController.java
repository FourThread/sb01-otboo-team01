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
        UUID requesterId = userDetails.getPayloadDto().userId();
        FollowDto dto = followService.createFollow(requesterId, request.followeeId());
        return ResponseEntity.status(201).body(dto);
    }


    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> getFollowSummary(
            @RequestParam UUID userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UUID requesterId = userDetails.getPayloadDto().userId();

        FollowSummaryDto summary = followService.getFollowSummary(userId, requesterId);
        return ResponseEntity.ok(summary);
    }


    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> deleteFollow(
            @PathVariable UUID followId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UUID requesterId = userDetails.getPayloadDto().userId();
        followService.deleteFollow(followId, requesterId);
        return ResponseEntity.noContent().build();
    }
}