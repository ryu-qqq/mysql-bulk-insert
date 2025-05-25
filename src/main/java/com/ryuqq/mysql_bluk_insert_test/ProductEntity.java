package com.ryuqq.mysql_bluk_insert_test;

import jakarta.persistence.*;

@Table(name = "PRODUCT_TEST")
@Entity
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PRODUCT_GROUP_ID", nullable = false)
    private long productGroupId;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    protected ProductEntity(){}

    public ProductEntity(Long id, long productGroupId, int quantity) {
        this.id = id;
        this.productGroupId = productGroupId;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public long getProductGroupId() {
        return productGroupId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setProductGroupId(long productGroupId) {
        this.productGroupId = productGroupId;
    }
}
