package com.ryuqq.mysql_bluk_insert_test;

import jakarta.persistence.*;

@Table(name = "PRODUCT_GROUP_TEST")
@Entity
public class ProductGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "PRODUCT_GROUP_NAME", nullable = false, length = 150)
    private String productGroupName;

    protected ProductGroupEntity(){}


    public ProductGroupEntity(long id, String productGroupName) {
        this.id = id;
        this.productGroupName = productGroupName;
    }

    public ProductGroupEntity(String productGroupName) {
        this.productGroupName = productGroupName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getProductGroupName() {
        return productGroupName;
    }

    public void setProductGroupName(String productGroupName) {
        this.productGroupName = productGroupName;
    }
}
