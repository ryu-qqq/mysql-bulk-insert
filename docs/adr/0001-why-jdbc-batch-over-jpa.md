# ADR-0001: JDBC Batch + LAST_INSERT_ID()를 JPA 대신 선택한 이유

**Status**: Accepted

## Context

일대다 애그리거트 (`ProductGroup` 1 — `Product` N) 를 대량 저장해야 하는 요구사항.
부모의 PK를 즉시 알아야 자식과 연결 가능하므로, "한꺼번에 저장하고 PK 추적" 전략이 필요.

먼저 JPA로 해결할 수 있는지 검토했으나 다음 한계 확인:

### Hibernate IDENTITY 전략에서 batch insert는 강제 비활성화됨

Hibernate User Guide:
> "Hibernate disables insert batching at the JDBC level transparently if you use an identity identifier generator."

이유:
- `IDENTITY` 전략은 INSERT 실행 → DB가 PK 할당 → JDBC가 PK 반환받음, 순서.
- Hibernate는 영속성 컨텍스트(persistence context)를 관리하기 위해 각 엔티티의 PK를 INSERT 직후 알아야 함.
- 따라서 batch로 묶을 수가 없음 — 단건 INSERT 강제.

`hibernate.jdbc.batch_size=100` 같은 설정을 줘도 IDENTITY 전략에서는 **무시됨**.

### 대안 ID 전략의 한계 (MySQL 기준)

| 전략 | MySQL에서 가능? | 평가 |
|------|-----------------|------|
| `IDENTITY` (AUTO_INCREMENT) | ✅ 가능 | Batch 불가 (위 이유) |
| `SEQUENCE` | ❌ 불가 | MySQL은 SEQUENCE 객체 자체가 없음 |
| `TABLE` (별도 시퀀스 테이블) | ✅ 가능 | 시퀀스 테이블 락 + 추가 라운드트립. 성능 페널티 큼 |
| `UUID` / `Snowflake` (앱에서 생성) | ✅ 가능 | Batch 가능. 다만 PK 크기/인덱스 트레이드오프 (→ ADR-0002) |

→ **MySQL 환경에서 JPA batch insert는 사실상 "앱에서 PK 생성" 외엔 선택지가 없음**

## Decision

**JDBC `batchUpdate` + `LAST_INSERT_ID()` 조합을 채택.**

근거:
1. AUTO_INCREMENT를 유지하면서도 batch insert 가능
2. MySQL InnoDB의 ID 블록 선점 특성을 활용 (`LAST_INSERT_ID()` 가 블록 시작 ID 반환)
3. 추가 라이브러리 없이 Spring JDBC 만으로 구현 가능
4. UUIDv7로 가지 않아도 됨 → 단일 MySQL 환경에서 PK 크기 페널티 회피 (→ ADR-0002)

## Consequences

### 좋은 점
- 약 6배 성능 개선 확인 (10k건 기준 8,545ms → 1,419ms)
- 멀티 스레드 환경에서도 세션 단위 격리로 안전 (검증 완료)
- AUTO_INCREMENT 의 모든 장점 (단조 증가, 인덱스 효율) 유지

### 나쁜 점 / 제약
- `rewriteBatchedStatements=true` 가 전제 조건 (→ ADR-0003)
- `LAST_INSERT_ID()`는 **세션(connection) 단위** 보장. 트랜잭션이 connection 경계를 넘으면 깨질 수 있음
- AUTO_INCREMENT lock mode가 `2` (interleaved) 일 경우 ID 블록 보장이 약해짐. 기본값 `1` 또는 `0` 권장
- JPA의 dirty checking, lazy loading 등을 포기하므로 도메인 모델과 영속성 모델 분리 필요

### 리스크
- 향후 DB가 MySQL에서 다른 RDBMS로 바뀌면 `LAST_INSERT_ID()` 동작이 다를 수 있음. 다만 PostgreSQL은 `RETURNING` 절로 더 깔끔하게 해결 가능
- Multi-master / 샤딩 환경으로 가면 AUTO_INCREMENT 자체가 문제. 그 시점엔 PK 전략 재검토 필요 (→ ADR-0002)

## 검증

- 실증 테스트: [`HibernateBatchInsertDisabledTest`](../../src/test/java/com/ryuqq/mysql_bluk_insert_test/HibernateBatchInsertDisabledTest.java)
  - `hibernate.jdbc.batch_size=100` 설정에서도 단건 INSERT 발생 확인
- 성능 비교: [`ProductGroupContextJpaTest`](../../src/test/java/com/ryuqq/mysql_bluk_insert_test/ProductGroupContextJpaTest.java) vs [`ProductGroupContextJdbcTest`](../../src/test/java/com/ryuqq/mysql_bluk_insert_test/ProductGroupContextJdbcTest.java)

## 참고

- [Hibernate User Guide — Batch](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#batch)
- [MySQL — InnoDB AUTO_INCREMENT Handling](https://dev.mysql.com/doc/refman/8.0/en/innodb-auto-increment-handling.html)
