package com.fourthread.ozang.module.domain.notification.entity;

import com.fourthread.ozang.module.common.converter.JsonMapConverter;
import com.fourthread.ozang.module.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;



@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private UUID receiverId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationLevel level;

    /**
     * =============== 새로운 필드 추가 ===============
     * 알림 메타데이터 (JSON 형태로 저장)
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    // Getter and Setter
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
