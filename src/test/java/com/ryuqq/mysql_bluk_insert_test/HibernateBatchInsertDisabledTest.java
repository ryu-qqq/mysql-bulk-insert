package com.ryuqq.mysql_bluk_insert_test;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hibernate IDENTITY 전략에서 batch insert가 실제로 비활성화됨을 실증.
 *
 * <p>{@code hibernate.jdbc.batch_size=100} 설정에도 불구하고
 * INSERT statement 수가 row 수와 거의 동일하게 발생함을 검증.
 *
 * <p>참고:
 * <ul>
 *   <li>ADR-0001: Why JDBC Batch over JPA (docs/adr/0001-why-jdbc-batch-over-jpa.md)</li>
 *   <li>Hibernate User Guide: "Hibernate disables insert batching at the JDBC level
 *       transparently if you use an identity identifier generator."</li>
 * </ul>
 */
@Import(JpaTestConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.jdbc.batch_size=100",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class HibernateBatchInsertDisabledTest extends AbstractIntegrationTest {

    private static final int ROW_COUNT = 100;
    private static final int CONFIGURED_BATCH_SIZE = 100;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProductGroupJpaRepository productGroupJpaRepository;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    @Transactional
    @DisplayName("IDENTITY 전략에서는 batch_size=100 설정해도 batch insert가 비활성화된다")
    void identityStrategy_disables_batch_insert_silently() {
        // given
        List<ProductGroupEntity> groups = IntStream.range(0, ROW_COUNT)
                .mapToObj(i -> new ProductGroupEntity("Group-" + i))
                .toList();

        // when
        productGroupJpaRepository.saveAll(groups);
        productGroupJpaRepository.flush();

        // then
        long entityInsertCount = statistics.getEntityInsertCount();
        long prepareStatementCount = statistics.getPrepareStatementCount();

        System.out.println("=== Hibernate Statistics ===");
        System.out.println("Configured batch_size  : " + CONFIGURED_BATCH_SIZE);
        System.out.println("Entity insert count    : " + entityInsertCount);
        System.out.println("Prepare statement count: " + prepareStatementCount);

        // 100건 모두 영속화되었는지 확인
        assertThat(entityInsertCount)
                .as("100건 모두 영속화되었어야 함")
                .isEqualTo(ROW_COUNT);

        // 핵심 검증:
        // batch가 실제로 작동했다면 prepare statement는 1~2개여야 함.
        // IDENTITY 전략에서는 batch가 강제 비활성화되어 row 수만큼 statement가 발생.
        assertThat(prepareStatementCount)
                .as("IDENTITY 전략에서 hibernate.jdbc.batch_size=%d 설정은 무시된다. " +
                        "→ %d건 INSERT 시 약 %d개의 prepared statement 발생 (1이 아님)",
                        CONFIGURED_BATCH_SIZE, ROW_COUNT, ROW_COUNT)
                .isGreaterThanOrEqualTo(ROW_COUNT);
    }
}
