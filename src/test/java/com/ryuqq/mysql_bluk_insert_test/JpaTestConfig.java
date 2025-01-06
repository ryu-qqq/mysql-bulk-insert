package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@TestConfiguration
public class JpaTestConfig {

    @Bean
    @Primary
    public ProductGroupPersistenceRepository testProductGroupRepository(ProductGroupJpaRepository productGroupJpaRepository) {
        return new ProductGroupJpaPersistenceRepository(productGroupJpaRepository);
    }

    @Bean
    @Primary
    public ProductPersistenceRepository testProductRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ProductJdbcRepository(namedParameterJdbcTemplate);
    }

}