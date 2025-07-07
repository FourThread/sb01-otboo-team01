package com.fourthread.ozang.module.domain.notification.controller;

import com.fourthread.ozang.module.domain.notification.dto.response.NotificationCursorResponse;
import com.fourthread.ozang.module.domain.notification.service.NotificationService;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
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
        UUID userId = userDetails.getPayloadDto().userId();
        return notificationService.findAllByReceiver(userId, cursor, idAfter, limit);
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID notificationId
    ) {
        UUID userId = userDetails.getPayloadDto().userId();
        notificationService.delete(userId, notificationId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

}