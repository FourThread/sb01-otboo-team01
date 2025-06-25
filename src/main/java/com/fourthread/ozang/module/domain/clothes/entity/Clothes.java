package com.fourthread.ozang.module.domain.clothes.entity;


import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clothes extends BaseUpdatableEntity {

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "owner_id")
    //private User owner;

    //굳이 연관관계 필요없이 아이디만 가져도 될듯
    //옷에서 유저정보를 얻진 않음
    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ClothesType type;

    //옷 객체 저장할때, ClothesAttribute 같이 저장
    //close 객체 삭제시 -> 이 리스트에 있는 ClothesAttribute 같이 삭제됨
    @OneToMany(mappedBy = "clothes", cascade = CascadeType.ALL)
    private List<ClothesAttribute> attributes = new ArrayList<>();



    //생성자에서 호출하자
    private void addAttributes(ClothesAttribute clothesAttribute) {
        attributes.add(clothesAttribute);
        clothesAttribute.assignClothes(this);
    }
}
