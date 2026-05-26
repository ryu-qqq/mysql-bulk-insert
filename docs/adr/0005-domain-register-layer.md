# ADR-0005: Register (Domain 진입) 레이어 유지 결정

**Status**: Accepted (작은 프로젝트라 over-engineering 가능성 인지하고 유지)

## Context

현재 코드 구조:

```
ProductGroupContextRegister  (Application Service)
  └─ ProductGroupRegister    (Domain 진입점)
       └─ ProductGroupPersistenceRepository  (Port 인터페이스)
            ├─ ProductGroupJdbcRepository           (JDBC Adapter)
            └─ ProductGroupJpaPersistenceRepository (JPA Adapter)
```

`ProductGroupRegister` 와 `ProductRegister` 는 현재 `saveAll(...)` 을 그대로 위임만 한다.
검토자가 **"한 단계 더 있는 이유가 뭔가요?"** 라고 물을 수 있는 구조이므로 의도를 명시한다.

## Decision

**유지한다.** 이유:

1. **헥사고날 스타일 분리** — 도메인 레이어가 영속성 인터페이스를 모르도록 한 단계 wrapper
2. **확장 지점 (extension point)** — 향후 도메인 검증, 이벤트 발행, 도메인 정책 등이 들어갈 위치
3. **테스트 격리** — 도메인 레이어 단위 테스트 시 영속성 mock 주입 지점 명확
4. **JDBC/JPA 비교가 본 프로젝트의 본질** — 영속성 Adapter 가 갈아끼워지는 패턴이 구조로 표현됨

## Consequences

### 좋은 점
- 향후 비즈니스 로직 추가 시 위치가 코드 구조로 정해져 있음
- 도메인/영속성 경계가 명시적
- JMH 벤치마크 (`JmhBenchmarkConfig`) 에서 같은 도메인 진입점에 JDBC/JPA Adapter 를 각각 주입해 비교하는 패턴이 자연스러움

### 나쁜 점 (정직하게)
- **현재 코드만 보면 1:1 delegate** → over-engineering 으로 보일 수 있음
- 작은 프로젝트(학습/실험 성격) 에서는 YAGNI 위배 가능
- 본 프로젝트의 가치는 "JDBC Batch + LAST_INSERT_ID 안정성 검증" 이라는 실험 자체이므로 이 레이어는 부가적

### 대안
- **제거 옵션**: `ProductGroupContextRegister` 가 `*PersistenceRepository` 를 직접 inject → 구조 단순화.
  의도가 진짜 도메인 분리가 아니었다면 이쪽이 더 정직한 선택.

본 프로젝트는 "실험 + 검증 + 트레이드오프 학습" 성격이므로 over-engineering 비용보다 패턴 학습 가치가 크다고 판단하여 유지. 다만 production 코드에서 같은 구조를 적용할 때는 비즈니스 로직 추가 시점까지 도메인 진입 레이어 도입을 미루는 편이 일반적으로 더 합리적이다.
