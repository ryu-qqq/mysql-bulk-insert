package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductGroupUuidJpaRepository extends JpaRepository<ProductGroupUuidEntity, UUID> {
}
