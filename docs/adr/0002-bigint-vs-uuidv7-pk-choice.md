# ADR-0002: BIGINT AUTO_INCREMENT vs UUIDv7 — PK 선택

**Status**: Accepted (단일 MySQL 환경 한정)

## Context

[ADR-0001](0001-why-jdbc-batch-over-jpa.md) 에서 "MySQL + JPA batch insert는 앱에서 PK 생성하는 경우에만 가능" 이라 결론냄.

그렇다면 **앱에서 PK를 만들고 JPA Batch로 가는 것**과 **DB AUTO_INCREMENT + JDBC Batch + `LAST_INSERT_ID()`** 중 어느 쪽이 우월한가?

자주 들어오는 질문:
> "그냥 UUIDv7나 Snowflake로 PK 만들면 JPA로도 배치 되고, 분산 환경 친화적이고, 외부 노출도 안전한데 왜 BIGINT 고집?"

답을 정리하기 위해 양쪽의 트레이드오프를 명시화.

## Decision

**현 프로젝트의 컨텍스트 (단일 MySQL, 내부 시스템, 분산 PK 발급 불필요) 에서는 BIGINT AUTO_INCREMENT를 유지.**

다른 컨텍스트에서는 결정이 달라질 수 있음 (아래 참조).

## 트레이드오프 매트릭스

| 항목 | BIGINT AUTO_INCREMENT | UUIDv7 |
|------|----------------------|--------|
| **크기** | 8 bytes | 16 bytes (**2배**) |
| **분산 PK 발급** | 약함 (DB 의존) | 강함 (앱에서 생성) |
| **PK 충돌 위험** | DB 단일 source → 0 | 천문학적으로 낮음 |
| **InnoDB 클러스터드 인덱스** | row 크기 작음 → 페이지당 row 多 | row 크기 큼 → 페이지당 row 少 |
| **Secondary 인덱스** | 모든 secondary가 PK 포함 (8B) | 모든 secondary가 PK 포함 (16B) → **인덱스 전체 크기 ~60% 증가** |
| **B-Tree fragmentation** | 단조 증가, 페이지 분할 최소 | UUIDv4: 심각 / UUIDv7: timestamp-prefix로 완화 |
| **buffer pool 효율** | 높음 | 낮음 (캐시 미스 ↑) |
| **외부 노출 안전성** | 약함 (enumerable, `/users/1`, `/users/2` 추측 가능) | 강함 |
| **디버깅/로그 가독성** | 좋음 | 나쁨 (16진수 36자) |
| **JPA Batch Insert** | ❌ (IDENTITY 한계) | ✅ |

## 컨텍스트별 권장

### 단일 MySQL, 내부 시스템 → **BIGINT**
- 인덱스 크기 페널티가 가장 큰 손실
- 외부 노출 PK가 아니므로 enumerable 문제 없음
- 분산 발급 불필요
- → 이 프로젝트가 여기 해당

### 분산 시스템, 멀티 마스터, 샤딩 → **UUIDv7 또는 Snowflake**
- AUTO_INCREMENT 자체가 동작 안 함 (마스터 간 충돌)
- 앱 레벨 PK 생성이 필수
- 인덱스 크기 페널티는 받아들일 수밖에 없음

### 외부 노출 PK (URL, API 응답) → **UUIDv7 또는 별도 슬러그**
- BIGINT 노출 시 enumerable 공격 위험
- 또는 BIGINT는 내부 PK로 유지하고 외부엔 별도 슬러그/UUID 노출

## Consequences

### 좋은 점 (현 결정)
- MySQL InnoDB 인덱스가 가장 효율적인 형태로 작동
- buffer pool 캐시 hit rate 최대
- 디버깅/로그가 사람 친화적

### 나쁜 점 / 제약
- JPA batch insert 불가 → JDBC로 우회 필요 (→ ADR-0001)
- 분산 환경으로 확장 시 재검토 필요
- API에 PK를 그대로 노출하면 enumerable 공격에 취약 — 외부 노출 시 추가 보호 (rate limit, 권한 체크, 별도 슬러그 등) 필요

### 리스크
- 트래픽이 매우 커지면 단일 AUTO_INCREMENT counter가 병목이 될 수 있음 (이 시점엔 샤딩 + UUIDv7 검토)
- "내부 시스템 → 외부 시스템" 전환 시 PK 전략 마이그레이션 필요

## 실측 결과 (본 프로젝트)

외부 출처(Percona)의 일반론을 본 프로젝트 환경에서 직접 측정하여 검증:

- **측정 코드**: [`IndexSizeMeasurement`](../../src/test/java/com/ryuqq/mysql_bluk_insert_test/IndexSizeMeasurement.java)
- **측정 결과**: [`docs/benchmarks/index-size-results.md`](../benchmarks/index-size-results.md)
- **JMH 성능 비교**: 3 시나리오 (BIGINT JDBC Batch / JPA IDENTITY / UUIDv7 JPA Batch) — [`JdbcVsJpaBenchmark`](../../src/jmh/java/com/ryuqq/mysql_bluk_insert_test/jmh/JdbcVsJpaBenchmark.java)

측정 환경:
- TestContainers MySQL 8.0
- 100,000 rows per table
- `ANALYZE TABLE` 으로 통계 갱신 후 `information_schema.tables` 조회

## 참고

- [Percona — UUIDs are Popular, but Bad for Performance](https://www.percona.com/blog/uuids-are-popular-but-bad-for-performance-lets-discuss/)
- [Percona — Storing UUID Values in MySQL Tables](https://www.percona.com/blog/store-uuid-optimized-way/)
- [RFC 9562 — UUIDv7 Specification](https://www.rfc-editor.org/rfc/rfc9562)
- [MySQL — InnoDB Index Types](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)
