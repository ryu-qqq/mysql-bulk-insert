package com.ryuqq.mysql_bluk_insert_test;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.BigDecimalRangeRandomizer;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;

import java.math.BigDecimal;
import java.util.Map;

import static org.jeasy.random.FieldPredicates.named;

public class EasyRandomUtils {
    private static final EasyRandom easyRandom;

    static {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .excludeField(named("id"))
                .randomize(String.class, new StringRandomizer(10))
                .randomize(Integer.class, new IntegerRangeRandomizer(0, 100));

        easyRandom = new EasyRandom(parameters);
    }

    public static EasyRandom getInstance() {
        return easyRandom;
    }
}
