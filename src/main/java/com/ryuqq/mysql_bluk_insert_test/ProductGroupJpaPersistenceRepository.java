package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class ProductGroupJpaPersistenceRepository implements ProductGroupPersistenceRepository {

    private final ProductGroupJpaRepository productGroupJpaRepository;

    public ProductGroupJpaPersistenceRepository(ProductGroupJpaRepository productGroupJpaRepository) {
        this.productGroupJpaRepository = productGroupJpaRepository;
    }

    @Override
    public List<Long> saveAll(List<ProductGroupEntity> productGroupEntities) {
        return productGroupJpaRepository.saveAll(productGroupEntities).stream()
                .map(ProductGroupEntity::getId)
                .toList();
    }

}
