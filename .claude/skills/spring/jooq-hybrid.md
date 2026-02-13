# jOOQ Hybrid Strategy (JPA + jOOQ)

JPA Cold Path + jOOQ Hot Path 하이브리드 데이터 액세스 패턴

## Quick Reference

```
데이터 액세스 선택
    │
    ├─ 단순 CRUD ──────────> JPA (Spring Data)
    │
    ├─ 동적 쿼리/검색 ────> JPA + QueryDSL
    │
    ├─ 고성능 동시성 ──────> jOOQ (Hot Path)
    │   ├─ 좌석 락/상태변경
    │   ├─ Bulk INSERT/UPDATE
    │   └─ Local Replica 쿼리
    │
    └─ 집계/통계 ──────────> jOOQ (type-safe SQL)
```

---

## 기술 선택 기준

| 경로 | 기술 | 사용처 | 이유 |
|------|------|--------|------|
| Cold Path | JPA + QueryDSL | Member CRUD, Game 조회 | ORM 편의성, 캐시, Lazy Loading |
| Hot Path | jOOQ | 좌석 락킹, Bulk init, Local Replica | Type-safe SQL, 직접 쿼리 제어, 성능 |

---

## Gradle 설정 (jOOQ + Flyway)

```gradle
plugins {
    id 'nu.studer.jooq' version '9.0'
    id 'org.flywaydb.flyway' version '10.x'
}

dependencies {
    implementation 'org.jooq:jooq'
    jooqGenerator 'org.postgresql:postgresql'
}

flyway {
    url = 'jdbc:postgresql://localhost:5432/booking_db'
    user = 'postgres'
    password = 'postgres'
    schemas = ['public']
}

jooq {
    configurations {
        main {
            generationTool {
                jdbc {
                    url = flyway.url
                    user = flyway.user
                    password = flyway.password
                }
                generator {
                    database {
                        name = 'org.jooq.meta.postgres.PostgresDatabase'
                        inputSchema = 'public'
                    }
                    generate {
                        daos = true
                        records = true
                        fluentSetters = true
                    }
                    target {
                        packageName = 'com.sportstix.booking.jooq.generated'
                        directory = 'src/main/java'
                    }
                }
            }
        }
    }
}

tasks.named('generateJooq').configure {
    dependsOn 'flywayMigrate'
    allInputsDeclared = true
}
```

---

## Hot Path 패턴

### 1. 좌석 락킹 (Pessimistic Lock + jOOQ)

```java
@Repository
@RequiredArgsConstructor
public class SeatLockJooqRepository {
    private final DSLContext dsl;

    /**
     * SELECT FOR UPDATE with NOWAIT
     * 이미 락이 걸린 좌석은 즉시 예외 발생
     */
    public LocalGameSeatRecord lockSeatForUpdate(Long gameSeatId) {
        return dsl.selectFrom(LOCAL_GAME_SEAT)
            .where(LOCAL_GAME_SEAT.ID.eq(gameSeatId))
            .and(LOCAL_GAME_SEAT.STATUS.eq("AVAILABLE"))
            .forUpdate()
            .noWait()
            .fetchOne();
    }

    /**
     * Batch status update (held → reserved)
     */
    public int updateSeatStatus(List<Long> seatIds, String status, Long bookingId) {
        return dsl.update(LOCAL_GAME_SEAT)
            .set(LOCAL_GAME_SEAT.STATUS, status)
            .set(LOCAL_GAME_SEAT.HELD_BY_BOOKING_ID, bookingId)
            .set(LOCAL_GAME_SEAT.HELD_AT, LocalDateTime.now())
            .set(LOCAL_GAME_SEAT.VERSION, LOCAL_GAME_SEAT.VERSION.add(1))
            .where(LOCAL_GAME_SEAT.ID.in(seatIds))
            .and(LOCAL_GAME_SEAT.VERSION.eq(
                dsl.select(LOCAL_GAME_SEAT.VERSION)
                   .from(LOCAL_GAME_SEAT)
                   .where(LOCAL_GAME_SEAT.ID.eq(seatIds.get(0)))
            ))
            .execute();
    }
}
```

### 2. Bulk 좌석 초기화 (25,000석)

```java
/**
 * game.seat.initialized 이벤트 수신 시
 * Game Service → Booking Service local_game_seat 동기화
 */
public void bulkInsertLocalSeats(List<GameSeatEvent> seats) {
    var insert = dsl.insertInto(LOCAL_GAME_SEAT,
        LOCAL_GAME_SEAT.ID,
        LOCAL_GAME_SEAT.GAME_ID,
        LOCAL_GAME_SEAT.SEAT_ID,
        LOCAL_GAME_SEAT.SECTION_NAME,
        LOCAL_GAME_SEAT.SEAT_LABEL,
        LOCAL_GAME_SEAT.SEAT_GRADE,
        LOCAL_GAME_SEAT.PRICE,
        LOCAL_GAME_SEAT.STATUS,
        LOCAL_GAME_SEAT.VERSION
    );

    for (var seat : seats) {
        insert = insert.values(
            seat.id(), seat.gameId(), seat.seatId(),
            seat.sectionName(), seat.seatLabel(), seat.seatGrade(),
            seat.price(), "AVAILABLE", 0
        );
    }

    insert.onConflict(LOCAL_GAME_SEAT.ID)
          .doUpdate()
          .set(LOCAL_GAME_SEAT.PRICE, excluded(LOCAL_GAME_SEAT.PRICE))
          .set(LOCAL_GAME_SEAT.STATUS, excluded(LOCAL_GAME_SEAT.STATUS))
          .execute();
}
```

### 3. Local Replica 조회

```java
/**
 * Booking Service에서 좌석 조회 (Game Service 호출 없이)
 */
public List<SeatAvailabilityDto> findAvailableSeats(Long gameId, String sectionName) {
    return dsl.select(
            LOCAL_GAME_SEAT.ID,
            LOCAL_GAME_SEAT.SEAT_LABEL,
            LOCAL_GAME_SEAT.SEAT_GRADE,
            LOCAL_GAME_SEAT.PRICE,
            LOCAL_GAME_SEAT.STATUS
        )
        .from(LOCAL_GAME_SEAT)
        .where(LOCAL_GAME_SEAT.GAME_ID.eq(gameId))
        .and(LOCAL_GAME_SEAT.SECTION_NAME.eq(sectionName))
        .and(LOCAL_GAME_SEAT.STATUS.eq("AVAILABLE"))
        .orderBy(LOCAL_GAME_SEAT.SEAT_LABEL)
        .fetchInto(SeatAvailabilityDto.class);
}
```

---

## JPA + jOOQ 공존 설정

```java
@Configuration
public class JooqConfig {

    @Bean
    public DSLContext dslContext(DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
```

- JPA `@Entity` 는 Cold Path CRUD에 사용
- jOOQ Generated 클래스는 Hot Path에 사용
- **같은 DataSource 공유**, 같은 트랜잭션 안에서 혼용 가능

---

## Optimistic Lock (jOOQ 방식)

```java
public boolean updateWithOptimisticLock(Long seatId, String newStatus, int expectedVersion) {
    int updated = dsl.update(LOCAL_GAME_SEAT)
        .set(LOCAL_GAME_SEAT.STATUS, newStatus)
        .set(LOCAL_GAME_SEAT.VERSION, expectedVersion + 1)
        .where(LOCAL_GAME_SEAT.ID.eq(seatId))
        .and(LOCAL_GAME_SEAT.VERSION.eq(expectedVersion))
        .execute();

    return updated == 1; // false → 다른 트랜잭션이 먼저 수정함
}
```

---

## Common Mistakes

| Mistake | Correct | Why |
|---------|---------|-----|
| jOOQ로 모든 쿼리 작성 | Hot Path만 jOOQ | 유지보수 비용 |
| JPA로 좌석 락킹 | jOOQ SELECT FOR UPDATE | 성능, NOWAIT 지원 |
| Batch INSERT를 루프로 | jOOQ multi-row INSERT | 25,000석 성능 |
| Generated 코드 수동 수정 | Flyway → jOOQ codegen 재생성 | 코드 동기화 |
| DSLContext를 static 사용 | Spring Bean 주입 | 트랜잭션 관리 |

---

## 관련 Skills
- `/spring-data` - JPA + QueryDSL (Cold Path)
- `/distributed-lock` - Redis 분산 락 (1st tier)
- `/concurrency-spring` - 동시성 패턴
- `/database-migration` - Flyway 마이그레이션
- `/kafka-patterns` - Local Replica 동기화 이벤트
