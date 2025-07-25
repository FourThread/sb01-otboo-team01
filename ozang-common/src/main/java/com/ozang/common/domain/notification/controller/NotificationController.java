package com.ozang.common.domain.notification.controller;

import com.ozang.common.domain.notification.dto.response.NotificationCursorResponse;
import com.ozang.common.domain.notification.service.NotificationService;
import com.ozang.common.domain.security.userdetails.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationCursorResponse getNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit
    ) {
        UUID requesterId = userDetails.getPayloadDto().userId();
        return notificationService.findAllByReceiver(requesterId, cursor, idAfter, limit);
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID notificationId
    ) {
        UUID requesterId = userDetails.getPayloadDto().userId();
        notificationService.delete(requesterId, notificationId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}