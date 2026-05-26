package com.ryuqq.mysql_bluk_insert_test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * 통합 테스트의 공통 베이스.
 *
 * <p>Singleton container 패턴 (marketplace 프로젝트와 동일):
 * <ul>
 *   <li>{@code static final} 필드 + {@code static { start() }} 로 JVM 수명 동안 한 번만 기동</li>
 *   <li>{@code @Testcontainers/@Container} 어노테이션은 testcontainers 2.x 에서 lifecycle 가 다르게 동작하여 사용하지 않음</li>
 *   <li>{@code withReuse(true)} 로 컨테이너를 JVM 사이에서도 재사용
 *     (활성화: {@code ~/.testcontainers.properties} 에 {@code testcontainers.reuse.enable=true})</li>
 * </ul>
 *
 * <p>참고: <a href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">Singleton Containers</a>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    public static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("ryuqq")
                    .withUsername("root")
                    .withPassword("root")
                    .withCommand(
                            "--character-set-server=utf8mb4",
                            "--collation-server=utf8mb4_unicode_ci",
                            // Linux 컨테이너의 MySQL 은 기본 case-sensitive.
                            // Spring Boot SpringPhysicalNamingStrategy 가 @Table(name="PRODUCT_GROUP_TEST")
                            // 를 product_group_test (소문자) 로 변환해 schema 생성하므로,
                            // 코드의 대문자 SQL 과 매칭되도록 case-insensitive 강제.
                            "--lower-case-table-names=1")
                    // 명시적 wait: "ready for connections" 로그 2번 (TCP + Unix socket) 까지 대기.
                    // JMH fork JVM 처럼 start() 직후 즉시 connect 하는 케이스에서 race 회피.
                    .waitingFor(
                            Wait.forLogMessage(".*ready for connections.*", 2)
                                    .withStartupTimeout(Duration.ofMinutes(3))
                    );
    // 의도적으로 withReuse(false): 매 JVM 새 컨테이너 → schema state 격리 보장
    // (reuse 시 context cache key 마다 ddl-auto=create 가 drop+recreate 하면서
    //  타이밍/cache 이슈로 PRODUCT_GROUP_TEST 가 누락되는 케이스 관찰됨)

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("storage.datasource.core.jdbc-url", MYSQL::getJdbcUrl);
        registry.add("storage.datasource.core.username", MYSQL::getUsername);
        registry.add("storage.datasource.core.password", MYSQL::getPassword);
        // 멀티 스레드 테스트 대비: 기본 pool=5 / timeout=1.1s 는
        // 100 스레드 동시 INSERT 시나리오에서 즉시 고갈 + timeout. 충분히 확대.
        registry.add("storage.datasource.core.maximum-pool-size", () -> "100");
        registry.add("storage.datasource.core.connection-timeout", () -> "30000");
        // 컨텍스트 캐시 별 schema 안정화: context 종료 시 drop, 시작 시 create.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
