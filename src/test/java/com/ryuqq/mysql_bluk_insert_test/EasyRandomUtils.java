package com.ryuqq.mysql_bluk_insert_test;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.range.IntegerRangeRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;

import static org.jeasy.random.FieldPredicates.named;

public final class EasyRandomUtils {

    public static final EasyRandom INSTANCE = build();

    private EasyRandomUtils() {
        // util — 인스턴스화 금지
    }

    private static EasyRandom build() {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .excludeField(named("id"))
                .excludeField(named("productGroupId"))
                .randomize(String.class, new StringRandomizer(10))
                .randomize(Integer.class, new IntegerRangeRandomizer(0, 100));
        return new EasyRandom(parameters);
    }
}
