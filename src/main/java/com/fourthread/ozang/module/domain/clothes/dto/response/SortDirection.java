package com.fourthread.ozang.module.domain.clothes.dto.response;

public enum SortDirection {
    ASCENDING, DESCENDING;

    public static SortDirection from(String value) {
        try {
            return SortDirection.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 정렬 방향: " + value); //TODO 커스텀 예외처리
        }
    }

    public boolean isDescending() {
        return this == DESCENDING;
    }
}