package com.fourthread.ozang.module.domain.follow.dto;

import java.util.UUID;

public record FollowCreateRequest(
        UUID followeeId
) {}