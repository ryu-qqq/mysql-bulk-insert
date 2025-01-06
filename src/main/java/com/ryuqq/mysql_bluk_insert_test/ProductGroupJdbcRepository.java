package com.ryuqq.mysql_bluk_insert_test;

import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

@Repository
public class ProductGroupJdbcRepository implements ProductGroupPersistenceRepository{

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProductGroupJdbcRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Transactional
    @Override
    public List<Long> saveAll(List<ProductGroupEntity> productGroupEntities) {
        int[] insertCounts = batchInsertProductGroups(productGroupEntities);

        int numberOfInsertedRows = insertCounts.length;
        List<Long> insertedIds = getInsertedIds(numberOfInsertedRows);
        return insertedIds;
    }




    private int[] batchInsertProductGroups(List<ProductGroupEntity> productGroupEntities) {
        String sql = "INSERT INTO PRODUCT_GROUP_TEST " +
                "(PRODUCT_GROUP_NAME) " +
                "VALUES (:product_group_name)";

        List<Map<String, Object>> batchValues = productGroupEntities.stream()
                .map(productGroup -> {
                    MapSqlParameterSource params = new MapSqlParameterSource()
                            .addValue("product_group_name", productGroup.getProductGroupName());
                    return params.getValues();
                })
                .toList();

        return namedParameterJdbcTemplate.batchUpdate(sql, batchValues.toArray(new Map[0]));
    }

    private List<Long> getInsertedIds(int numberOfRows) {
        long firstInsertedId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        return LongStream.range(firstInsertedId, firstInsertedId + numberOfRows)
                .boxed()
                .toList();
    }

    public List<ProductGroupEntity> findByProductGroupNames(List<String> productGroupNames) {
        String sql = "SELECT ID, PRODUCT_GROUP_NAME FROM PRODUCT_GROUP_TEST WHERE PRODUCT_GROUP_NAME IN (:names)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("names", productGroupNames);

        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) ->
                new ProductGroupEntity(rs.getLong("ID"), rs.getString("PRODUCT_GROUP_NAME"))
        );
    }


}
