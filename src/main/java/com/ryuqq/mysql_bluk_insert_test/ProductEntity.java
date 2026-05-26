package com.ryuqq.mysql_bluk_insert_test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PRODUCT_TEST")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PRODUCT_GROUP_ID", nullable = false)
    private Long productGroupId;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    protected ProductEntity() {
    }

    public ProductEntity(Long id, Long productGroupId, int quantity) {
        this.id = id;
        this.productGroupId = productGroupId;
        this.quantity = quantity;
    }

    public ProductEntity(Long productGroupId, int quantity) {
        this(null, productGroupId, quantity);
    }

    public ProductEntity withId(Long id) {
        return new ProductEntity(id, this.productGroupId, this.quantity);
    }

    public Long getId() {
        return id;
    }

    public Long getProductGroupId() {
        return productGroupId;
    }

    public int getQuantity() {
        return quantity;
    }
}
