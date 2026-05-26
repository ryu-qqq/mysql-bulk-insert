package com.ryuqq.mysql_bluk_insert_test;

import java.util.List;

public record ProductGroupContextCommand(
        ProductGroupCommand productGroupCommand,
        List<ProductCommand> productCommands
) {
}
