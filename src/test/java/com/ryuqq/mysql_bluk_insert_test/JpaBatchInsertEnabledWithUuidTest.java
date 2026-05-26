package com.ryuqq.mysql_bluk_insert_test;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UUIDv7 PK 사용 시 JPA Batch Insert 가 실제로 작동함을 실증.
 *
 * <p>{@link HibernateBatchInsertDisabledTest} 와 짝을 이룬다:
 * <ul>
 *   <li>{@link HibernateBatchInsertDisabledTest} — IDENTITY 전략에서는 batch 강제 비활성화</li>
 *   <li>본 테스트 — UUIDv7 (앱 레벨 PK 생성) 에서는 batch 활성화 가능</li>
 * </ul>
 *
 * <p>같은 {@code batch_size=100} 설정에서 prepared statement 수가
 * row 수보다 훨씬 적게 발생함을 검증한다.
 *
 * <p>참고: ADR-0001 (JDBC Batch 채택 이유), ADR-0002 (BIGINT vs UUIDv7 트레이드오프).
 */
@Import(JpaTestConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.jdbc.batch_size=100",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class JpaBatchInsertEnabledWithUuidTest extends AbstractIntegrationTest {

    private static final int ROW_COUNT = 100;
    private static final int CONFIGURED_BATCH_SIZE = 100;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProductGroupUuidJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("TRUNCATE TABLE PRODUCT_GROUP_UUID_TEST");
    }

    @Test
    @Transactional
    @DisplayName("UUIDv7 PK 사용 시 batch_size=100 설정이 실제 작동한다 (IDENTITY 한계 우회)")
    void uuidv7Pk_enables_real_batch_insert() {
        // given — 앱 레벨에서 PK 미리 생성
        List<ProductGroupUuidEntity> groups = IntStream.range(0, ROW_COUNT)
                .mapToObj(i -> new ProductGroupUuidEntity(
                        UuidCreator.getTimeOrderedEpoch(),
                        "Group-" + i))
                .toList();

        // when
        repository.saveAll(groups);
        repository.flush();

        // then
        long entityInsertCount = statistics.getEntityInsertCount();
        long prepareStatementCount = statistics.getPrepareStatementCount();

        System.out.println("=== Hibernate Statistics (UUIDv7 시나리오) ===");
        System.out.println("Configured batch_size  : " + CONFIGURED_BATCH_SIZE);
        System.out.println("Entity insert count    : " + entityInsertCount);
        System.out.println("Prepare statement count: " + prepareStatementCount);

        assertThat(entityInsertCount)
                .as("100 건 모두 영속화")
                .isEqualTo(ROW_COUNT);

        // 핵심 검증:
        // IDENTITY 전략 (HibernateBatchInsertDisabledTest) 에서는 statement 가 row 수와 동일.
        // UUIDv7 (앱 레벨 PK) 에서는 batch 가 활성화되어 훨씬 적은 statement 가 발생해야 한다.
        assertThat(prepareStatementCount)
                .as("UUID PK 는 IDENTITY 한계가 없으므로 batch insert 가 활성화된다. " +
                        "%d 건 INSERT 가 batch_size=%d 로 묶여 prepared statement 가 row 수보다 크게 적어야 함.",
                        ROW_COUNT, CONFIGURED_BATCH_SIZE)
                .isLessThan(ROW_COUNT);
    }
}
