package com.fourthread.ozang.module.domain.notification.sse.controller;


import com.fourthread.ozang.module.domain.notification.sse.service.SseService;
import com.fourthread.ozang.module.domain.security.userdetails.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(value = "LastEventId", required = false) UUID lastEventId
    ) {
        UUID userId = userDetails.getPayloadDto().userId();
        return sseService.connect(userId, lastEventId);
    }
}
