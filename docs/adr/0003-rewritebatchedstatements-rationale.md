# ADR-0003: `rewriteBatchedStatements=true` 가 필수인 이유

**Status**: Accepted

## Context

이 프로젝트의 핵심 동작 — **JDBC Batch + `LAST_INSERT_ID()` 로 PK 추적** — 은 다음 설정이 있어야만 작동:

```properties
spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
```

이 설정이 빠지면 모든 검증 결과 (성능 6배, PK 추적 안정성) 가 깨짐.
왜 그런지 명시화.

## Decision

**`rewriteBatchedStatements=true` 를 필수 전제 조건으로 명시. README 와 환경 설정 파일 모두에 강조 표시.**

## 이 설정이 하는 일

MySQL Connector/J의 동작:

- **`rewriteBatchedStatements=false` (기본값)**:
  ```sql
  -- batch에 1000건 넣어도 실제로는:
  INSERT INTO product_group (...) VALUES (...);  -- statement 1
  INSERT INTO product_group (...) VALUES (...);  -- statement 2
  ...
  INSERT INTO product_group (...) VALUES (...);  -- statement 1000
  ```
  JDBC가 batch API를 호출해도 네트워크 레벨에서는 개별 statement로 나감.

- **`rewriteBatchedStatements=true`**:
  ```sql
  -- batch 1000건이 하나의 multi-value INSERT로 재작성됨:
  INSERT INTO product_group (...) VALUES (...), (...), (...), ... (...);
  ```
  단일 statement로 묶임.

## 왜 이게 `LAST_INSERT_ID()` 안정성에 결정적인가

MySQL InnoDB의 `LAST_INSERT_ID()` 동작:

> "LAST_INSERT_ID() returns the **first** automatically generated value that was successfully inserted **for an AUTO_INCREMENT column as a result of the most recently executed INSERT statement**."

핵심: **"INSERT statement 단위"** 로 보장.

### `rewriteBatchedStatements=false` 의 경우
- 1,000건이 1,000개의 INSERT statement로 실행됨
- `LAST_INSERT_ID()` 는 **마지막 statement의 ID** 만 반환
- 1~999번째의 ID는 잃어버림
- 또는 statement 사이에 다른 connection 의 INSERT가 끼어들면 ID 순서 깨짐 가능

### `rewriteBatchedStatements=true` 의 경우
- 1,000건이 **하나의 INSERT statement** 로 재작성됨
- MySQL InnoDB는 해당 statement에 대해 **ID 블록을 미리 선점** (lock mode 1 기본값)
- `LAST_INSERT_ID()` 는 그 블록의 **시작 ID** 반환
- 시작 ID + 0, +1, +2, ... +999 로 나머지 PK 계산 가능
- statement 안에서 ID 충돌 0, 다른 connection 영향 0

## Consequences

### 좋은 점
- PK 추적이 결정적(deterministic)으로 작동
- 네트워크 라운드트립 1번으로 끝남 → 성능 추가 향상
- 멀티 스레드 환경에서도 connection 단위 격리 보장

### 나쁜 점 / 제약
- **`max_allowed_packet`** 제약 — multi-value INSERT 한 줄이 너무 길어지면 잘림. 매우 큰 batch (수만 건)는 분할 필요
- 일부 오래된 MySQL 클라이언트는 multi-value INSERT 동작이 다를 수 있음 (8.0 기준 정상)
- 디버그 로그가 한 줄로 길게 찍힘 → 가독성 ↓

### 리스크
- **이 설정이 빠진 채로 운영 환경 배포되면 silent failure** — 에러는 안 나는데 PK가 틀린 값으로 매핑됨. 데이터 무결성 사고로 이어질 수 있음.
- 대응: CI/통합 테스트에서 이 옵션 누락 시 실패하는 헬스 체크 권장

## 검증

- [`ProductGroupJdbcRepositoryTest`](../../src/test/java/com/ryuqq/mysql_bluk_insert_test/ProductGroupJdbcRepositoryTest.java) — 100 스레드 × 1,000건 동시 삽입 시 PK 추적 무결성

## 참고

- [MySQL Connector/J — Configuration Properties](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-performance-extensions.html)
- [MySQL — LAST_INSERT_ID() Function](https://dev.mysql.com/doc/refman/8.4/en/information-functions.html#function_last-insert-id)
- [MySQL — InnoDB AUTO_INCREMENT Lock Modes](https://dev.mysql.com/doc/refman/8.0/en/innodb-auto-increment-handling.html#innodb-auto-increment-lock-modes)
