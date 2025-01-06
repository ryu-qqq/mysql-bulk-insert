package com.ryuqq.mysql_bluk_insert_test;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

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
    public void saveProductGroupWithProducts(List<ProductGroupEntity> productGroups, List<ProductEntity> products) {
        List<Long> productGroupIds = productGroupRegister.saveAll(productGroups);

        List<ProductEntity> updatedProducts = products.stream()
                .peek(product -> {
                    int index = products.indexOf(product) % productGroupIds.size();
                    product.setProductGroupId(productGroupIds.get(index));
                })
                .toList();

        productRegister.saveAll(updatedProducts);
    }

}
