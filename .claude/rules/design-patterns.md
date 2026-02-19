# Design Pattern Rules (Java 21 + Spring Boot 3.3)

## Effective Java

### Rule 1: Static Factory Method over Constructor
- public constructor 대신 static factory method 사용
- 이름으로 의도를 드러냄 (`of`, `from`, `create`, `valueOf`)
```java
// BAD
new BookingResponse(booking.getId(), booking.getStatus());

// GOOD
BookingResponse.from(booking);
```

### Rule 2: Builder for 4+ parameters
- 생성자 파라미터가 4개 이상이면 Builder 패턴 사용
- Lombok `@Builder` 활용, 필수 파라미터는 생성자로 강제
```java
@Builder(builderMethodName = "builder")
public class GameCreateRequest {
    @NotNull private final String title;
    @NotNull private final Long stadiumId;
    private final LocalDateTime openTime;
    private final Integer maxCapacity;
}
```

### Rule 3: Immutable DTO
- DTO는 반드시 불변 (record 또는 final fields)
- Java 21이므로 record 우선 사용
```java
// GOOD - record (Java 16+)
public record GameResponse(Long id, String title, GameStatus status) {
    public static GameResponse from(Game game) {
        return new GameResponse(game.getId(), game.getTitle(), game.getStatus());
    }
}
```

### Rule 4: Optional return, never parameter
- 반환값에만 Optional 사용, 메서드 파라미터/필드에 사용 금지
- `Optional.get()` 금지 → `orElseThrow()` 사용
```java
// BAD
public void process(Optional<Game> game) { ... }

// GOOD
public Optional<Game> findByTitle(String title) { ... }
game.orElseThrow(() -> new GameNotFoundException(gameId));
```

### Rule 5: Enum over constant String/int
- 상태값, 타입 구분에 String/int 상수 대신 Enum 사용
- Enum에 행위를 부여 (Strategy 패턴 결합)
```java
public enum BookingStatus {
    PENDING { public boolean isCancellable() { return true; } },
    CONFIRMED { public boolean isCancellable() { return true; } },
    CANCELLED { public boolean isCancellable() { return false; } };

    public abstract boolean isCancellable();
}
```

### Rule 6: try-with-resources for AutoCloseable
- Stream, Connection 등 AutoCloseable 자원은 반드시 try-with-resources

### Rule 7: Defensive copy for mutable fields
- Entity/DTO에서 List, Map 등 mutable 컬렉션 반환 시 unmodifiable copy
```java
public List<GameSeat> getSeats() {
    return Collections.unmodifiableList(seats);
}
```

## GoF Patterns (프로젝트 적용)

### Rule 8: Strategy Pattern for payment/notification
- 결제 수단, 알림 채널 등 교체 가능한 로직은 Strategy 패턴
- Spring의 DI와 결합하여 구현체 자동 주입
```java
public interface PaymentStrategy {
    PaymentResult process(PaymentRequest request);
}

@Component("CARD")
public class CardPaymentStrategy implements PaymentStrategy { ... }

// 사용측에서 Map injection
@Autowired Map<String, PaymentStrategy> strategies;
```

### Rule 9: Template Method for common workflows
- 공통 흐름이 있고 세부 단계만 다를 때 사용
- Kafka consumer 처리, 예약 검증 등에 적용
```java
public abstract class AbstractEventConsumer<T> {
    public final void handle(ConsumerRecord<String, T> record) {
        validate(record);
        T event = deserialize(record);
        process(event);
        acknowledge(record);
    }
    protected abstract void process(T event);
}
```

### Rule 10: Observer via Kafka events
- 서비스 간 Observer 패턴은 직접 호출 금지 → Kafka event로 구현
- ApplicationEventPublisher는 서비스 내부 이벤트에만 사용
```java
// Inter-service: Kafka (NOT REST)
kafkaTemplate.send("ticket.booking.created", bookingEvent);

// Intra-service: Spring Event
applicationEventPublisher.publishEvent(new BookingCreatedInternalEvent(booking));
```

### Rule 11: Facade for complex orchestration
- 3개 이상의 서비스를 조합하는 로직은 Facade로 분리
- Controller → Facade → Services 구조
```java
@Service
public class BookingFacade {
    private final SeatLockService seatLockService;
    private final BookingService bookingService;
    private final PaymentEventProducer paymentProducer;

    public BookingResponse createBooking(BookingRequest request) { ... }
}
```

## Spring-Specific Patterns

### Rule 12: @Transactional boundary awareness
- @Transactional은 Service 레이어에만 선언
- Self-invocation 금지: 같은 클래스 내 @Transactional 메서드 호출 시 프록시 우회됨
```java
// BAD - self invocation, proxy bypassed
@Service
public class BookingService {
    public void process() { this.save(); } // @Transactional 무시됨
    @Transactional public void save() { ... }
}

// GOOD - separate bean
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingPersistenceService persistenceService;
    public void process() { persistenceService.save(); }
}
```

### Rule 13: Constructor injection only
- Field injection (`@Autowired` on field) 금지
- Lombok `@RequiredArgsConstructor` + `private final` 필드 사용
```java
// BAD
@Autowired private GameRepository gameRepository;

// GOOD
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
}
```

### Rule 14: Exception hierarchy
- 비즈니스 예외는 공통 BaseException 상속
- ErrorCode enum으로 관리, @RestControllerAdvice에서 일괄 처리
```java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}
// throw new BusinessException(ErrorCode.SEAT_ALREADY_HELD);
```

### Rule 15: Entity ↔ DTO 변환은 mapper/static factory
- Entity를 Controller/외부에 직접 노출 금지
- Entity → DTO 변환은 DTO의 static factory 또는 MapStruct
- LazyInitializationException 방지: 트랜잭션 안에서 DTO 변환 완료

### Rule 16: Domain event over direct dependency
- 서비스 간 강결합 방지: 직접 의존 대신 도메인 이벤트
- Outbox 패턴으로 이벤트 발행 보장 (booking-service 참고)

## Java 21 Features

### Rule 17: Use sealed classes for domain hierarchies
```java
public sealed interface PaymentEvent
    permits PaymentCompleted, PaymentFailed, PaymentRefunded { }
```

### Rule 18: Pattern matching for instanceof
```java
// BAD
if (event instanceof PaymentCompleted) {
    PaymentCompleted completed = (PaymentCompleted) event;
}

// GOOD
if (event instanceof PaymentCompleted completed) {
    process(completed);
}
```

### Rule 19: Switch expressions with pattern matching
```java
return switch (status) {
    case PENDING -> "Waiting for payment";
    case CONFIRMED -> "Booking confirmed";
    case CANCELLED -> "Booking cancelled";
};
```

### Rule 20: Virtual Threads for blocking I/O
- Spring Boot 3.2+ `spring.threads.virtual.enabled=true`
- Kafka consumer, DB 호출 등 blocking I/O에 적합
- CPU-bound 작업에는 사용하지 않음
