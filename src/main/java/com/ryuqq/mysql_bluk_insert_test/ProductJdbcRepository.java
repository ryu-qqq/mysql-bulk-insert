package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ProductJdbcRepository implements ProductPersistenceRepository{

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProductJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public void saveAll(List<ProductEntity> productEntities) {
        batchInsertProducts(productEntities);
    }


    private int[] batchInsertProducts(List<ProductEntity> productEntities) {
        String sql = "INSERT INTO PRODUCT_TEST " +
                "(PRODUCT_GROUP_ID, QUANTITY) " +
                "VALUES (:productGroupId, :quantity)";

        List<Map<String, Object>> batchValues = productEntities.stream()
                .map(product -> {
                    MapSqlParameterSource params = new MapSqlParameterSource()
                            .addValue("productGroupId", product.getProductGroupId())
                            .addValue("quantity", product.getQuantity());
                    return params.getValues();
                })
                .toList();

        return namedParameterJdbcTemplate.batchUpdate(sql, batchValues.toArray(new Map[0]));
    }



}
