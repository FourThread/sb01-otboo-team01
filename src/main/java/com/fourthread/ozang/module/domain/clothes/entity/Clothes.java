package com.fourthread.ozang.module.domain.clothes.entity;


import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothes extends BaseUpdatableEntity {
    
    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ClothesType type;

    @Column(length = 2048)
    private String imageUrl;

    //옷 객체 저장할때, ClothesAttribute 같이 저장
    //close 객체 삭제시 -> 이 리스트에 있는 ClothesAttribute 같이 삭제됨
    @Builder.Default
    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothesAttribute> attributes = new ArrayList<>();


    public void addAttribute(ClothesAttribute clothesAttribute) {
        attributes.add(clothesAttribute);
        clothesAttribute.assignClothes(this);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateType(ClothesType type) {
        this.type = type;
    }

    public void clearAttributes() {
        for (ClothesAttribute attr : attributes) {
            attr.assignClothes(null);
        }
        this.attributes.clear();
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
