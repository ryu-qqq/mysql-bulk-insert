package com.ryuqq.mysql_bluk_insert_test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(JdbcTestConfig.class)
class ProductGroupJdbcRepositoryTest {

    @Autowired
    private ProductGroupJdbcRepository productGroupJdbcRepository;

    @Test
    void testConcurrentInserts() throws InterruptedException {
        int threadCount = 100; // 스레드 수
        int groupsPerThread = 1000; // 각 스레드가 삽입할 그룹 개수

        // 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 결과 저장 리스트
        List<Future<List<ProductGroupEntity>>> futureResults = new ArrayList<>();

        // 각 스레드에서 실행할 작업 정의
        IntStream.range(0, threadCount).forEach(threadIndex -> {
            Future<List<ProductGroupEntity>> future = executorService.submit(() -> {
                // 고유 데이터 생성 (스레드별 구분을 위해 이름에 스레드 ID 포함)
                List<ProductGroupEntity> productGroups = EasyRandomUtils.getInstance()
                        .objects(ProductGroupEntity.class, groupsPerThread)
                        .peek(group -> group.setProductGroupName("Thread-" + threadIndex + "-" + group.getProductGroupName()))
                        .toList();

                // 저장
                List<Long> insertedIds = productGroupJdbcRepository.saveAll(productGroups);

                // 저장된 ID를 엔티티에 매핑
                for (int i = 0; i < productGroups.size(); i++) {
                    productGroups.get(i).setId(insertedIds.get(i));
                }

                return productGroups;
            });
            futureResults.add(future);
        });

        // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        // 각 스레드의 결과 검증
        for (int threadIndex = 0; threadIndex < futureResults.size(); threadIndex++) {
            try {
                List<ProductGroupEntity> insertedGroups = futureResults.get(threadIndex).get();

                // 삽입된 상품명 리스트 생성
                List<String> insertedNames = insertedGroups.stream()
                        .map(ProductGroupEntity::getProductGroupName)
                        .toList();

                // 상품명으로 조회
                List<ProductGroupEntity> foundGroups = productGroupJdbcRepository.findByProductGroupNames(insertedNames);

                // ID로 매핑
                Map<Long, ProductGroupEntity> insertedGroupMap = insertedGroups.stream()
                        .collect(Collectors.toMap(ProductGroupEntity::getId, Function.identity()));

                Map<Long, ProductGroupEntity> foundGroupMap = foundGroups.stream()
                        .collect(Collectors.toMap(ProductGroupEntity::getId, Function.identity()));

                // ID와 이름 검증
                insertedGroupMap.forEach((id, insertedGroup) -> {
                    ProductGroupEntity foundGroup = foundGroupMap.get(id);
                    assertNotNull(foundGroup, "ID " + id + "로 조회된 상품 그룹이 없습니다.");
                    assertEquals(insertedGroup.getProductGroupName(), foundGroup.getProductGroupName(),
                            "ID " + id + "의 상품 그룹 이름이 일치하지 않습니다.");
                });

                System.out.println("Thread " + threadIndex + " passed verification.");
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }


}