package com.ryuqq.mysql_bluk_insert_test;

import java.util.List;

public interface ProductPersistenceRepository {

    void saveAll(List<ProductEntity> productEntities);

}
