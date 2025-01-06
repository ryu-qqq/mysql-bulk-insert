package com.ryuqq.mysql_bluk_insert_test;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Import(JpaTestConfig.class)
class ProductGroupContextJpaTest {

    @Autowired
    private ProductGroupContextRegister productGroupContextRegister;

    private List<ProductGroupEntity> productGroups;
    private List<ProductEntity> products;

    @BeforeEach
    void setUp() {
        EasyRandom easyRandom = EasyRandomUtils.getInstance();
        productGroups = easyRandom.objects(ProductGroupEntity.class, 1000).toList();
        products = easyRandom.objects(ProductEntity.class, 10000).toList();
    }

    @Test
    void saveProductGroupWithProducts() {
        long startTime = System.currentTimeMillis();
        productGroupContextRegister.saveProductGroupWithProducts(productGroups, products);
        long endTime = System.currentTimeMillis();

        System.out.println("[JPA] Execution Time: " + (endTime - startTime) + "ms");
    }
}

