package com.ryuqq.mysql_bluk_insert_test.jmh;

import com.ryuqq.mysql_bluk_insert_test.JdbcTestConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * JMH 벤치마크 전용 Spring 설정.
 *
 * <p>{@link JdbcTestConfig} 를 import 하여 {@code ProductGroupPersistenceRepository}
 * 의 {@code @Primary} 충돌을 해결한다 (JDBC/JPA 구현체 둘 다 자동 등록되므로 명시 필요).
 *
 * <p>JMH 벤치마크는 다음 3개의 Repository 를 직접 inject 받아 사용한다:
 * <ul>
 *   <li>{@code ProductGroupJdbcRepository} — JDBC Batch + BIGINT AUTO_INCREMENT</li>
 *   <li>{@code ProductGroupJpaPersistenceRepository} — JPA IDENTITY 단건</li>
 *   <li>{@code ProductGroupUuidJpaRepository} — JPA Batch + UUIDv7</li>
 * </ul>
 */
@TestConfiguration
@Import(JdbcTestConfig.class)
public class JmhBenchmarkConfig {
}
