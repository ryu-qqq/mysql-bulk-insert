package com.ryuqq.mysql_bluk_insert_test;

public class ProductGroupCommand{

    Long id;
    String productGroupName;

    public ProductGroupCommand(Long id, String productGroupName) {
        this.id = id;
        this.productGroupName = productGroupName;
    }

    public ProductGroupEntity toEntity(){
        return new ProductGroupEntity(id, productGroupName);
    }
}
