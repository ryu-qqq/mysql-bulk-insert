package com.ryuqq.mysql_bluk_insert_test;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    List<ProductGroupContextCommand> productGroupContextCommands;

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000})
    void saveProductGroupWithProducts(int count) {
        // 해당 count에 맞는 테스트 데이터 생성
        productGroupContextCommands = ProductGroupContextCommandFactory.createContextCommand(count);

        long startTime = System.currentTimeMillis();
        productGroupContextRegister.saveProductGroupWithProducts(productGroupContextCommands);
        long endTime = System.currentTimeMillis();

        System.out.println("[JPA] Execution Time for " + count + " commands: " + (endTime - startTime) + "ms");
    }
}

