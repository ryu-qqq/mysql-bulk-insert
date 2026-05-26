# Index Size Measurement Results

ADR-0002 의 "UUIDv7 PK 가 InnoDB 인덱스 크기에 미치는 영향" 주장에 대한 실측 기록.

측정 방법:
- TestContainers MySQL 8.0
- 동일 row 수 시드 후 `ANALYZE TABLE` → `information_schema.tables` 조회
- 측정 코드: [`IndexSizeMeasurement.java`](../../src/test/java/com/ryuqq/mysql_bluk_insert_test/IndexSizeMeasurement.java)

실행:
```bash
./gradlew test --tests IndexSizeMeasurement -PrunMeasurement=true
# 또는 IDE 에서 @Disabled 제거 후 단일 실행
```

---

(첫 실행 시 자동으로 결과가 아래에 append 됩니다.)
