package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductGroupRegister {

    private final ProductGroupPersistenceRepository productGroupPersistenceRepository;

    public ProductGroupRegister(ProductGroupPersistenceRepository productGroupPersistenceRepository) {
        this.productGroupPersistenceRepository = productGroupPersistenceRepository;
    }

    public List<Long> saveAll(List<ProductGroupEntity> productGroupEntities){
        return productGroupPersistenceRepository.saveAll(productGroupEntities);
    }

}
