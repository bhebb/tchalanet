# Backend Testing — Idempotency

Use this guidance when testing idempotent endpoints (especially sell ticket).

## Required scenarios

For each idempotent endpoint:

1. **Missing key** → 400 Bad Request
2. **First request** → creates resource, returns 201/200 + resource
3. **Same key + same payload** → replays same resource/result, returns 200 + same resource
4. **Same key + different payload** → 409 Conflict payload mismatch
5. **In-progress record** → 409 Conflict in progress (if applicable)

## Test structure

```java
@Nested
class IdempotencyTests {
  
  @Test
  void missingIdempotencyKey_returns400() { }
  
  @Test
  void firstRequest_createsResource() { }
  
  @Test
  void replayWithSamePayload_returnsSameResource() { }
  
  @Test
  void replayWithDifferentPayload_returns409() { }
  
  @Test
  void inProgressRequest_returns409() { }
}
```

## Idempotency key rules

- Use stable hash of command payload + tenant + requesting user.
- Do not include timestamp/request ID.
- Store processed requests with RLS tenant isolation.
- Replayed request must return exact same resource (same ID, same data).

## Integration test scope

Use real database when testing idempotency storage/lookup. Use Testcontainers if necessary.

Mock downstream side-effects (email, events) to verify they are not duplicated on replay.
