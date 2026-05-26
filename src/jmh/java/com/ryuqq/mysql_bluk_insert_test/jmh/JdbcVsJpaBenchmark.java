package com.ryuqq.mysql_bluk_insert_test.jmh;

import com.github.f4b6a3.uuid.UuidCreator;
import com.ryuqq.mysql_bluk_insert_test.AbstractIntegrationTest;
import com.ryuqq.mysql_bluk_insert_test.MysqlBlukInsertTestApplication;
import com.ryuqq.mysql_bluk_insert_test.ProductGroupEntity;
import com.ryuqq.mysql_bluk_insert_test.ProductGroupJdbcRepository;
import com.ryuqq.mysql_bluk_insert_test.ProductGroupJpaPersistenceRepository;
import com.ryuqq.mysql_bluk_insert_test.ProductGroupUuidEntity;
import com.ryuqq.mysql_bluk_insert_test.ProductGroupUuidJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 3가지 PK 전략 정식 벤치마크 (모두 단일 테이블 INSERT — Apple-to-Apple 비교).
 *
 * <p>시나리오:
 * <ol>
 *   <li>{@code jdbc_batch_insert} — JDBC Batch + BIGINT AUTO_INCREMENT + LAST_INSERT_ID()</li>
 *   <li>{@code jpa_identity_insert} — JPA IDENTITY 단건 INSERT (batch 강제 비활성화)</li>
 *   <li>{@code jpa_uuidv7_batch_insert} — JPA Batch + UUIDv7 (앱 레벨 PK 생성)</li>
 * </ol>
 *
 * <p>측정값으로 ADR-0001 (JDBC 선택 이유), ADR-0002 (BIGINT vs UUIDv7 트레이드오프)
 * 의 결정 근거가 실측 데이터로 뒷받침된다.
 *
 * <p>실행: {@code ./gradlew jmh}
 * <br>결과: {@code build/results/jmh/results.json}
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class JdbcVsJpaBenchmark {

    private ConfigurableApplicationContext context;

    private ProductGroupJdbcRepository jdbcRepository;
    private ProductGroupJpaPersistenceRepository jpaRepository;
    private ProductGroupUuidJpaRepository uuidRepository;
    private JdbcTemplate jdbcTemplate;

    @Param({"100", "1000", "10000"})
    private int rowCount;

    private List<ProductGroupEntity> bigIntGroups;
    private List<ProductGroupUuidEntity> uuidGroups;

    @Setup(Level.Trial)
    public void setupContext() {
        String url = "jdbc:mysql://localhost:3306/ryuqq?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String pwd = "root";
        try {
            System.err.println(">>> [JMH] Starting Spring context against " + url);
            // command line args (--key=value) — application.yml 의 빈 값 을 확실히 override
            // (properties() = setDefaultProperties = application.yml 보다 priority 낮음)
            context = new SpringApplicationBuilder(
                    MysqlBlukInsertTestApplication.class,
                    JmhBenchmarkConfig.class
            )
                    .profiles("test")
                    .run(
                            "--storage.datasource.core.jdbc-url=" + url,
                            "--storage.datasource.core.username=" + user,
                            "--storage.datasource.core.password=" + pwd,
                            "--storage.datasource.core.maximum-pool-size=100",
                            "--storage.datasource.core.connection-timeout=30000",
                            "--spring.jpa.properties.hibernate.jdbc.batch_size=100",
                            "--spring.jpa.properties.hibernate.order_inserts=true",
                            "--spring.jpa.properties.hibernate.order_updates=true",
                            "--spring.jpa.hibernate.ddl-auto=create-drop"
                    );
            System.err.println(">>> [JMH] Spring context started");
            jdbcRepository = context.getBean(ProductGroupJdbcRepository.class);
            jpaRepository = context.getBean(ProductGroupJpaPersistenceRepository.class);
            uuidRepository = context.getBean(ProductGroupUuidJpaRepository.class);
            jdbcTemplate = context.getBean(JdbcTemplate.class);
            System.err.println(">>> [JMH] Beans wired");
        } catch (Throwable t) {
            System.err.println(">>> [JMH] SETUP FAILED: " + t);
            t.printStackTrace(System.err);
            throw new RuntimeException(t);
        }
    }

    @Setup(Level.Invocation)
    public void setupData() {
        // invocation 사이 데이터 누적 방지 — 측정 일관성 보장
        jdbcTemplate.execute("TRUNCATE TABLE PRODUCT_GROUP_TEST");
        jdbcTemplate.execute("TRUNCATE TABLE PRODUCT_GROUP_UUID_TEST");

        bigIntGroups = IntStream.range(0, rowCount)
                .mapToObj(i -> new ProductGroupEntity("Group-" + i))
                .toList();
        uuidGroups = IntStream.range(0, rowCount)
                .mapToObj(i -> new ProductGroupUuidEntity(
                        UuidCreator.getTimeOrderedEpoch(),
                        "Group-" + i))
                .toList();
    }

    @Benchmark
    public List<?> jdbc_batch_insert() {
        return jdbcRepository.saveAll(bigIntGroups);
    }

    @Benchmark
    public List<?> jpa_identity_insert() {
        return jpaRepository.saveAll(bigIntGroups);
    }

    @Benchmark
    public List<ProductGroupUuidEntity> jpa_uuidv7_batch_insert() {
        return uuidRepository.saveAll(uuidGroups);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }
}
