package com.ryuqq.mysql_bluk_insert_test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * JDBC Batch + {@code LAST_INSERT_ID()} 의 멀티 스레드 환경 동작 검증.
 *
 * <p>두 시나리오:
 * <ol>
 *   <li>모든 스레드 정상 — PK 추적이 스레드 간 섞이지 않는지</li>
 *   <li>일부 스레드 실패 — 실패 스레드만 롤백되고 나머지는 영향 없는지</li>
 * </ol>
 */
@Import(JdbcTestConfig.class)
class ProductGroupJdbcRepositoryTest extends AbstractIntegrationTest {

    private static final int THREAD_COUNT = 100;
    private static final int GROUPS_PER_THREAD = 1_000;
    private static final int FAILING_THREAD_INDEX = 50;
    private static final long AWAIT_TIMEOUT_SECONDS = 60;

    @Autowired
    private ProductGroupJdbcRepository productGroupJdbcRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("TRUNCATE TABLE PRODUCT_TEST");
        jdbcTemplate.execute("TRUNCATE TABLE PRODUCT_GROUP_TEST");
    }

    @Test
    @DisplayName("100 스레드 × 1,000건 동시 삽입 시 LAST_INSERT_ID()는 각 스레드의 PK를 정확히 추적한다")
    void concurrentInserts_trackPrimaryKeysCorrectly() throws InterruptedException {
        List<List<ProductGroupEntity>> results =
                runConcurrentInserts(THREAD_COUNT, GROUPS_PER_THREAD, idx -> false);

        assertThat(results).hasSize(THREAD_COUNT);
        results.forEach(this::verifyPersistedDataMatches);
    }

    @Test
    @DisplayName("멀티 스레드 중 한 스레드만 실패 시 나머지 스레드 데이터는 정상 저장된다")
    void concurrentInserts_isolateFailedThread() throws InterruptedException {
        List<List<ProductGroupEntity>> results = runConcurrentInserts(
                THREAD_COUNT, GROUPS_PER_THREAD,
                idx -> idx == FAILING_THREAD_INDEX
        );

        assertThat(results)
                .as("실패 스레드를 제외한 결과만 수집되어야 함")
                .hasSize(THREAD_COUNT - 1);
        results.forEach(this::verifyPersistedDataMatches);
    }

    /**
     * threadCount 개 스레드를 띄워 각각 groupsPerThread 건씩 batch insert.
     * shouldFail 이 true 인 인덱스의 스레드는 예외 발생 → 결과에서 제외.
     */
    private List<List<ProductGroupEntity>> runConcurrentInserts(
            int threadCount,
            int groupsPerThread,
            IntPredicate shouldFail
    ) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<List<ProductGroupEntity>>> futures = new ArrayList<>(threadCount);

        IntStream.range(0, threadCount).forEach(idx ->
                futures.add(executor.submit(() -> {
                    if (shouldFail.test(idx)) {
                        throw new IllegalStateException("Intentional failure: thread " + idx);
                    }
                    return insertOneThreadWorth(idx, groupsPerThread);
                }))
        );

        executor.shutdown();
        boolean terminated = executor.awaitTermination(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(terminated)
                .as("ExecutorService should terminate within %d seconds", AWAIT_TIMEOUT_SECONDS)
                .isTrue();

        return collectSuccessfulResults(futures, shouldFail);
    }

    private List<ProductGroupEntity> insertOneThreadWorth(int threadIdx, int count) {
        List<ProductGroupEntity> groups = IntStream.range(0, count)
                .mapToObj(i -> new ProductGroupEntity("Thread-" + threadIdx + "-Group-" + i))
                .toList();

        List<Long> insertedIds = productGroupJdbcRepository.saveAll(groups);

        return IntStream.range(0, groups.size())
                .mapToObj(i -> groups.get(i).withId(insertedIds.get(i)))
                .toList();
    }

    private List<List<ProductGroupEntity>> collectSuccessfulResults(
            List<Future<List<ProductGroupEntity>>> futures,
            IntPredicate shouldFail
    ) {
        List<List<ProductGroupEntity>> results = new ArrayList<>();
        for (int idx = 0; idx < futures.size(); idx++) {
            Future<List<ProductGroupEntity>> future = futures.get(idx);
            if (shouldFail.test(idx)) {
                assertThatFutureFailedAsExpected(future, idx);
                continue;
            }
            try {
                results.add(future.get());
            } catch (ExecutionException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                fail("Thread %d should have succeeded but threw %s".formatted(idx, e.getCause()));
            }
        }
        return results;
    }

    private void assertThatFutureFailedAsExpected(Future<List<ProductGroupEntity>> future, int idx) {
        try {
            future.get();
            fail("Thread %d was expected to fail but succeeded".formatted(idx));
        } catch (ExecutionException expected) {
            // expected — 실패해야 함
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test was interrupted while collecting results");
        }
    }

    private void verifyPersistedDataMatches(List<ProductGroupEntity> insertedGroups) {
        List<String> names = insertedGroups.stream()
                .map(ProductGroupEntity::getProductGroupName)
                .toList();

        List<ProductGroupEntity> found = productGroupJdbcRepository.findByProductGroupNames(names);

        Map<Long, ProductGroupEntity> insertedById = insertedGroups.stream()
                .collect(Collectors.toMap(ProductGroupEntity::getId, Function.identity()));
        Map<Long, ProductGroupEntity> foundById = found.stream()
                .collect(Collectors.toMap(ProductGroupEntity::getId, Function.identity()));

        assertThat(foundById.keySet())
                .as("저장된 모든 PK가 DB에서 조회되어야 함")
                .containsAll(insertedById.keySet());

        insertedById.forEach((id, expected) ->
                assertThat(foundById.get(id))
                        .as("ID %d 의 이름이 일치해야 함", id)
                        .isNotNull()
                        .extracting(ProductGroupEntity::getProductGroupName)
                        .isEqualTo(expected.getProductGroupName())
        );
    }
}
