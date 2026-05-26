package com.ryuqq.mysql_bluk_insert_test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PRODUCT_GROUP_TEST")
public class ProductGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PRODUCT_GROUP_NAME", nullable = false, length = 150)
    private String productGroupName;

    protected ProductGroupEntity() {
    }

    public ProductGroupEntity(Long id, String productGroupName) {
        this.id = id;
        this.productGroupName = productGroupName;
    }

    public ProductGroupEntity(String productGroupName) {
        this(null, productGroupName);
    }

    /**
     * INSERT 후 DB 가 할당한 PK 를 받아 새 인스턴스를 만든다.
     * Entity 불변성을 유지하면서 LAST_INSERT_ID() 결과를 매핑하는 패턴.
     */
    public ProductGroupEntity withId(Long id) {
        return new ProductGroupEntity(id, this.productGroupName);
    }

    public Long getId() {
        return id;
    }

    public String getProductGroupName() {
        return productGroupName;
    }
}
