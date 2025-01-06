package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@TestConfiguration
public class JdbcTestConfig {

    @Bean
    @Primary
    public ProductGroupPersistenceRepository testProductGroupRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JdbcTemplate jdbcTemplate) {
        return new ProductGroupJdbcRepository(jdbcTemplate, namedParameterJdbcTemplate);
    }

    @Bean
    @Primary
    public ProductPersistenceRepository testProductRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ProductJdbcRepository(namedParameterJdbcTemplate);
    }

}
