package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductRegister {

    private final ProductPersistenceRepository productPersistenceRepository;

    public ProductRegister(ProductPersistenceRepository productPersistenceRepository) {
        this.productPersistenceRepository = productPersistenceRepository;
    }

    public void saveAll(List<ProductEntity> productEntities){
        productPersistenceRepository.saveAll(productEntities);
    }




}
