package com.ryuqq.mysql_bluk_insert_test;

import java.util.ArrayList;
import java.util.List;

import org.jeasy.random.EasyRandom;

public class ProductGroupContextCommandFactory {

    public static List<ProductGroupContextCommand> createContextCommand(int size){
        EasyRandom easyRandom = EasyRandomUtils.getInstance();

        List<ProductGroupContextCommand> commands = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            // ProductGroupCommand 생성
            ProductGroupCommand productGroupCommand = easyRandom.nextObject(ProductGroupCommand.class);

            // 10개의 ProductCommand 생성
            List<ProductCommand> productCommands = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                ProductCommand productCommand = easyRandom.nextObject(ProductCommand.class);
                productCommands.add(productCommand);
            }
            commands.add(new ProductGroupContextCommand(productGroupCommand, productCommands));
        }

        return commands;
    }

}
