package com.ryuqq.mysql_bluk_insert_test;

import java.util.List;

public class ProductGroupContextCommand{

    private ProductGroupCommand productGroupCommand;
    private List<ProductCommand> productCommands;

    public ProductGroupContextCommand(ProductGroupCommand productGroupCommand, List<ProductCommand> productCommands) {
        this.productGroupCommand = productGroupCommand;
        this.productCommands = productCommands;
    }

    public ProductGroupCommand getProductGroupCommand() {
        return productGroupCommand;
    }

    public List<ProductCommand> getProductCommands() {
        return productCommands;
    }
}
