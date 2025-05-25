package com.ryuqq.mysql_bluk_insert_test;

public class ProductCommand{

    private Long id;
    private Long productGroupId;
    private int quantity;

    public ProductCommand(Long id, Long productGroupId, int quantity) {
        this.id = id;
        this.productGroupId = productGroupId;
        this.quantity = quantity;
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

    public ProductEntity toEntity(Long productGroupId){
        return new ProductEntity(null, productGroupId, quantity);
    }

}
