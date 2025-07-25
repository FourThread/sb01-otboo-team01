package com.fourthread.ozang.domain.clothes.entity;


import com.fourthread.ozang.domain.BaseUpdatableEntity;
import com.fourthread.ozang.domain.clothes.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
