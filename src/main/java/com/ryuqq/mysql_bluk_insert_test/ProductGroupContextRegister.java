package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                .map(context -> context.productGroupCommand().toEntity())
                .toList();

        List<Long> productGroupIds = productGroupRegister.saveAll(productGroupEntities);

        List<ProductEntity> products = new ArrayList<>();

        for (int i = 0; i < productGroupIds.size(); i++) {
            Long productGroupId = productGroupIds.get(i);
            List<ProductEntity> productEntities = productGroupContextCommands.get(i)
                    .productCommands()
                    .stream()
                    .map(p -> p.toEntity(productGroupId))
                    .toList();

            products.addAll(productEntities);
        }

        productRegister.saveAll(products);
    }
}
