package com.fourthread.ozang.module.domain.weather.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record KakaoLocalResponse(

    @JsonProperty("meta")
    Meta meta,

    @JsonProperty("documents")
    List<Document> documents

) {

    public record Meta(
        @JsonProperty("total_count")
        int totalCount,

        @JsonProperty("pageable_count")
        int pageableCount,

        @JsonProperty("is_end")
        boolean isEnd
    ) {}

    public record Document(
        @JsonProperty("region_type")
        String regionType,

        @JsonProperty("address_name")
        String addressName,

        @JsonProperty("region_1depth_name")
        String region1DepthName,

        @JsonProperty("region_2depth_name")
        String region2DepthName,

        @JsonProperty("region_3depth_name")
        String region3DepthName,

        @JsonProperty("region_4depth_name")
        String region4DepthName,

        @JsonProperty("code")
        String code,

        @JsonProperty("x")
        double x,

        @JsonProperty("y")
        double y
    ) {}
}
