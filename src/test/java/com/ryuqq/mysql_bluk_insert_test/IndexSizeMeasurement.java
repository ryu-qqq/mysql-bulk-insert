package com.ryuqq.mysql_bluk_insert_test;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

/**
 * BIGINT AUTO_INCREMENT vs UUIDv7 PK 의 InnoDB 인덱스 크기 실측.
 *
 * <p>ADR-0002 의 트레이드오프 주장 ("UUIDv7 → 인덱스 크기 ~60% 증가") 을
 * 같은 환경에서 동일 row 수로 시드한 뒤 INFORMATION_SCHEMA 로 실측한다.
 *
 * <p>수동 실행: {@code ./gradlew test --tests IndexSizeMeasurement -DenableMeasurement=true}
 * <br>또는 IDE 에서 직접 실행 ({@code @Disabled} 제거하지 않고 단일 메서드 run).
 *
 * <p>결과는 콘솔 + {@code docs/benchmarks/index-size-results.md} 에 append.
 */
@Import(JdbcTestConfig.class)
@Disabled("수동 실행 전용 — 100k 시드 + 측정에 약 1~2분 소요. ADR-0002 실측 데이터 갱신 시 활성화.")
class IndexSizeMeasurement extends AbstractIntegrationTest {

    private static final int ROW_COUNT = 100_000;
    private static final int CHUNK_SIZE = 10_000;
    private static final Path RESULTS_PATH = Path.of("docs/benchmarks/index-size-results.md");

    @Autowired
    private ProductGroupJdbcRepository jdbcRepository;

    @Autowired
    private ProductGroupUuidJpaRepository uuidRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("BIGINT vs UUIDv7 PK 인덱스 크기 실측 (100,000 rows)")
    void measureIndexSizes() throws Exception {
        seedBigIntTable();
        seedUuidTable();
        forceMysqlStatistics();

        TableStats bigInt = queryTableStats("PRODUCT_GROUP_TEST");
        TableStats uuid = queryTableStats("PRODUCT_GROUP_UUID_TEST");

        printResults(bigInt, uuid);
        appendToResultsFile(bigInt, uuid);
    }

    private void seedBigIntTable() {
        for (int start = 0; start < ROW_COUNT; start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, ROW_COUNT);
            List<ProductGroupEntity> chunk = IntStream.range(start, end)
                    .mapToObj(i -> new ProductGroupEntity("Group-" + i))
                    .toList();
            jdbcRepository.saveAll(chunk);
        }
    }

    private void seedUuidTable() {
        for (int start = 0; start < ROW_COUNT; start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, ROW_COUNT);
            List<ProductGroupUuidEntity> chunk = IntStream.range(start, end)
                    .mapToObj(i -> new ProductGroupUuidEntity(
                            UuidCreator.getTimeOrderedEpoch(),
                            "Group-" + i))
                    .toList();
            uuidRepository.saveAll(chunk);
        }
    }

    /**
     * MySQL 의 information_schema 통계는 sampling 기반이라 시드 직후엔 부정확할 수 있다.
     * ANALYZE TABLE 로 강제 갱신해 더 정확한 측정값을 얻는다.
     */
    private void forceMysqlStatistics() {
        jdbcTemplate.execute("ANALYZE TABLE PRODUCT_GROUP_TEST");
        jdbcTemplate.execute("ANALYZE TABLE PRODUCT_GROUP_UUID_TEST");
    }

    private TableStats queryTableStats(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT table_name, table_rows, data_length, index_length " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?",
                (rs, rowNum) -> new TableStats(
                        rs.getString("table_name"),
                        rs.getLong("table_rows"),
                        rs.getLong("data_length"),
                        rs.getLong("index_length")
                ),
                tableName
        );
    }

    private void printResults(TableStats bigInt, TableStats uuid) {
        System.out.println();
        System.out.println("================================================================");
        System.out.println(" Index Size Measurement — BIGINT vs UUIDv7 PK");
        System.out.println(" Rows per table: " + ROW_COUNT);
        System.out.println("================================================================");
        System.out.printf(" %-30s %15s %15s %15s%n",
                "Table", "rows", "data_length", "index_length");
        System.out.printf(" %-30s %15d %15s %15s%n",
                bigInt.tableName(), bigInt.rows(),
                humanReadable(bigInt.dataLength()), humanReadable(bigInt.indexLength()));
        System.out.printf(" %-30s %15d %15s %15s%n",
                uuid.tableName(), uuid.rows(),
                humanReadable(uuid.dataLength()), humanReadable(uuid.indexLength()));
        System.out.println();
        System.out.printf(" Data length ratio  (UUID / BIGINT) : %.2fx%n",
                (double) uuid.dataLength() / bigInt.dataLength());
        System.out.printf(" Index length ratio (UUID / BIGINT) : %.2fx%n",
                (double) uuid.indexLength() / Math.max(bigInt.indexLength(), 1));
        System.out.println("================================================================");
    }

    private void appendToResultsFile(TableStats bigInt, TableStats uuid) throws Exception {
        Files.createDirectories(RESULTS_PATH.getParent());

        String header = """

                ## Run %s

                | 테이블 | rows | data_length | index_length |
                |--------|-----:|------------:|-------------:|
                """.formatted(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String row1 = "| `%s` | %d | %s | %s |%n".formatted(
                bigInt.tableName(), bigInt.rows(),
                humanReadable(bigInt.dataLength()), humanReadable(bigInt.indexLength()));
        String row2 = "| `%s` | %d | %s | %s |%n".formatted(
                uuid.tableName(), uuid.rows(),
                humanReadable(uuid.dataLength()), humanReadable(uuid.indexLength()));

        String footer = """

                - Data length ratio  (UUID / BIGINT): **%.2fx**
                - Index length ratio (UUID / BIGINT): **%.2fx**
                """.formatted(
                (double) uuid.dataLength() / bigInt.dataLength(),
                (double) uuid.indexLength() / Math.max(bigInt.indexLength(), 1));

        if (!Files.exists(RESULTS_PATH)) {
            Files.writeString(RESULTS_PATH,
                    "# Index Size Measurement Results\n\n" +
                            "ADR-0002 의 'UUIDv7 PK 가 InnoDB 인덱스 크기에 미치는 영향' 주장에 대한 실측 기록.\n");
        }
        Files.writeString(RESULTS_PATH, header + row1 + row2 + footer,
                StandardOpenOption.APPEND);
    }

    private static String humanReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return "%.1f KB".formatted(bytes / 1024.0);
        return "%.1f MB".formatted(bytes / (1024.0 * 1024));
    }

    record TableStats(String tableName, long rows, long dataLength, long indexLength) {
    }
}
