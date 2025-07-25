package com.fourthread.ozang.module.domain.clothes.entity;

import com.fourthread.ozang.module.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clothes_attributes")
public class ClothesAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id")
    private Clothes clothes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    private ClothesAttributeDefinition definition;

    @Column(nullable = false)
    private String attributeValue; //value 예약어라서 수정

    public ClothesAttribute(ClothesAttributeDefinition definition, String value) {
        this.definition = definition;
        this.attributeValue = value;
    }

    protected void assignClothes(Clothes clothes) {
        this.clothes = clothes;
    }
}
