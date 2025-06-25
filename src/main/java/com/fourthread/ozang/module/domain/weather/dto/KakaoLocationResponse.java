package com.fourthread.ozang.module.domain.weather.dto;

import java.util.List;

public record KakaoLocationResponse(
    List<Document> documents
) {
    public record Document(
        String region1DepthName,
        String region2DepthName,
        String region3DepthName
    ) {}
}