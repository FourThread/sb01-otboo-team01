package com.fourthread.ozang.module.domain.clothes.entity;


import com.fourthread.ozang.module.domain.BaseUpdatableEntity;
import com.fourthread.ozang.module.domain.clothes.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
@Table(name = "clothes_attribute_definitions")
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


/*    //의상속성정의에서 의상속성으로 갈 일이 있나? 아직은 필요없어 보임
    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothesAttribute> attributes = new ArrayList<>();*/
}
