package com.fourthread.ozang.module.domain.clothes.dto.response;

public enum SortBy {
    NAME, ID;

    public static SortBy from(String value) {
        try {
            return SortBy.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 정렬 필드: " + value); //TODO 커스텀 예외처리
        }
    }
}