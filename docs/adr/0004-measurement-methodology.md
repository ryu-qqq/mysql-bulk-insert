# ADR-0004: 측정 방법론 — JMH 정식 벤치마크 도입

**Status**: Accepted

## Context

초기 측정은 다음과 같이 단발성으로 수행됨:

```java
long start = System.currentTimeMillis();
register.saveProductGroupWithProducts(commands);
long end = System.currentTimeMillis();
System.out.println("Execution time: " + (end - start) + "ms");
```

이 측정 방식의 한계 (검토자가 즉시 지적할 수 있는 것):

1. **단발 측정** — 분산/표준편차 없음. 한 번 빠르고 한 번 느리면 어느 게 진짜?
2. **JIT 워밍업 누락** — 첫 ~10,000 호출은 인터프리터 모드. 측정값에 워밍업 시간이 섞임
3. **Dead Code Elimination** — 결과를 안 쓰면 컴파일러가 호출을 통째로 제거. 0ms 같은 거짓 결과
4. **Constant Folding** — 입력이 상수면 컴파일 시점에 계산
5. **GC / Compaction 영향** — 측정 중 GC 발생 시 spike
6. **이전 측정의 캐시 효과** — 같은 JVM에서 여러 시나리오 돌리면 서로 영향

→ "JPA 대비 4배 빠르다" 같은 결론의 통계적 신뢰도가 낮음.

## Decision

**JMH (Java Microbenchmark Harness) 정식 도입.**

JMH 는 OpenJDK 팀이 만든 공식 마이크로벤치마크 도구로, 위 6가지 함정을 의식적으로 회피하는 인프라를 제공한다.

구체 위치:
- 벤치마크 클래스: [`JdbcVsJpaBenchmark`](../../src/jmh/java/com/ryuqq/mysql_bluk_insert_test/jmh/JdbcVsJpaBenchmark.java)
- 빌드 설정: [`build.gradle`](../../build.gradle) `jmh { ... }` 블록
- 실행: `./gradlew jmh`
- 결과: `build/results/jmh/results.json` (자동 SVG 변환은 [Phase 4 워크플로우](../../.github/workflows/benchmark.yml))

## Methodology

| 항목 | 값 | 근거 |
|------|----|----|
| `@BenchmarkMode` | `Mode.AverageTime` | 호출당 평균 시간 — INSERT 작업 비교에 적합 |
| `@OutputTimeUnit` | `MILLISECONDS` | 결과 가독성 |
| `@Warmup` | 3 iterations × 3s | JIT 인터프리터 → 컴파일 전환 안정화 |
| `@Measurement` | 5 iterations × 5s | 평균 + 표준편차 산출 |
| `@Fork` | 1 (`-Xms2G -Xmx2G`) | 새 JVM 1회 시작 — 이전 측정 캐시 영향 차단 |
| `@Param("rowCount")` | 100, 1000, 10000 | 데이터 크기에 따른 스케일 곡선 |
| `@State(Scope.Benchmark)` | Spring context 공유 | 컨텍스트 부팅 비용 분산 |
| `return` value | 반드시 반환 | Dead Code Elimination 방지 |

### 시나리오 (3개)

| Benchmark | PK 전략 | 영속성 | 측정 의도 |
|-----------|--------|--------|----------|
| `jdbc_batch_insert` | BIGINT AUTO_INCREMENT | JDBC Batch + LAST_INSERT_ID() | 본 프로젝트의 채택안 |
| `jpa_identity_insert` | BIGINT AUTO_INCREMENT | JPA `saveAll` | IDENTITY 한계 (ADR-0001) |
| `jpa_uuidv7_batch_insert` | UUIDv7 (앱 생성) | JPA Batch | UUIDv7 우회 비교 (ADR-0002) |

총 측정: **3 시나리오 × 3 rowCount × 5 iteration = 45 측정** (워밍업 별도)

## Consequences

### 좋은 점
- **통계적 신뢰** — 평균 + 표준편차 (± Error) 함께 제공
- **JIT 안정화** — 워밍업으로 인터프리터 / 컴파일 분리
- **DCE 방지** — `@Benchmark` 메서드의 return 을 JMH 가 `Blackhole` 처리
- **재현 가능** — `@Fork(1)` 로 새 JVM 매번 시작
- **CI 자동 갱신** — [`benchmark.yml`](../../.github/workflows/benchmark.yml) 가 매월 1일 실행 → SVG 차트 commit

### 나쁜 점 / 제약
- **실행 시간 ~10분** — Spring context 부팅 (~30s) + 워밍업/측정 × 9 조합
- **CI 비용** — 매 push 실행은 부담. 월 1회 자동 + 수동 트리거로 절충
- **테이블 정리 안 함** — 매 measurement 사이에 TRUNCATE 안 함 → 데이터 누적. INSERT 시간 측정에 미치는 영향은 미미 (인덱스 깊이 증가 약간) 하지만 엄밀히는 한계
- **단일 호스트** — 네트워크 RTT 변수 없음 (실제 분산 환경과 다를 수 있음)
- **MySQL 한 가지 버전** — 8.0.36 기준. 다른 버전에선 다를 수 있음

### 리스크
- JMH 워밍업이 부족하면 실제 hot 상태 도달 전 측정 가능 → 결과에 노이즈
- DB connection pool 크기 영향 (현재 HikariCP max=5) → 동시 측정 시 병목

### 미래 작업 (선택)
- `IndexSizeMeasurement` CI 통합 (현재 수동 실행)
- 더 큰 `rowCount` (100k, 1M) 추가 — 시간 부담 vs 신뢰
- `Mode.SampleTime` 으로 p50 / p95 / p99 분포 측정
- 다양한 MySQL 버전 매트릭스 (8.0.x, 8.4.x)
