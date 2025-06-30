package com.fourthread.ozang.module.domain.clothes.repository;

import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClothesRepository extends JpaRepository<Clothes, UUID> {
}
