package com.fourthread.ozang.core.domain.clothes.entity;


import com.fourthread.ozang.core.domain.BaseUpdatableEntity;
import com.fourthread.ozang.core.domain.clothes.StringListConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clothes_attribute_definitions", schema = "public")
public class ClothesAttributeDefinition extends BaseUpdatableEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")  // optional
    private List<String> selectableValues = new ArrayList<>();

    public void update(String name, List<String> selectableValues) {
        this.name = name;
        this.selectableValues = selectableValues;
    }

}
