package com.ryuqq.mysql_bluk_insert_test;

import java.util.List;

public interface ProductGroupPersistenceRepository {

    List<Long> saveAll(List<ProductGroupEntity> productGroupEntities);

}
