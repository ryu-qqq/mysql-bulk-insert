package com.ryuqq.mysql_bluk_insert_test;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductGroupContextRegister {

    private final ProductGroupRegister productGroupRegister;
    private final ProductRegister productRegister;

    public ProductGroupContextRegister(ProductGroupRegister productGroupRegister, ProductRegister productRegister) {
        this.productGroupRegister = productGroupRegister;
        this.productRegister = productRegister;
    }

    @Transactional
    public void saveProductGroupWithProducts(List<ProductGroupContextCommand> productGroupContextCommands) {

        List<ProductGroupEntity> productGroupEntities = productGroupContextCommands.stream()
            .map(productGroupContextCommand -> productGroupContextCommand.getProductGroupCommand().toEntity())
            .toList();


        List<Long> productGroupIds = productGroupRegister.saveAll(productGroupEntities);

        List<ProductEntity> products = new ArrayList<>();

        for (int i = 0; i < productGroupIds.size(); i++) {
            Long productGroupId = productGroupIds.get(i);
            List<ProductEntity> productEntities = productGroupContextCommands.get(i)
                .getProductCommands()
                .stream()
                .map(p -> p.toEntity(productGroupId))
                .toList();

            products.addAll(productEntities);
        }

        productRegister.saveAll(products);
    }

}
