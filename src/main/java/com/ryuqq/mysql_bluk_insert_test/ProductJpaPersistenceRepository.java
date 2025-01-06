package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductJpaPersistenceRepository implements ProductPersistenceRepository{

    private final ProductJpaRepository productJpaRepository;

    public ProductJpaPersistenceRepository(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public void saveAll(List<ProductEntity> productEntities) {
        productJpaRepository.saveAll(productEntities);
    }
}
