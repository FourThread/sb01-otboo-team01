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
public class ClothesAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothes_id")
    private Clothes clothes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    private ClothesAttributeDefinition definition;

    @Column(nullable = false)
    private String value;

    public ClothesAttribute(ClothesAttributeDefinition definition, String value) {
        this.definition = definition;
        this.value = value;
    }

    protected void assignClothes(Clothes clothes) {
        this.clothes = clothes;
    }
}
