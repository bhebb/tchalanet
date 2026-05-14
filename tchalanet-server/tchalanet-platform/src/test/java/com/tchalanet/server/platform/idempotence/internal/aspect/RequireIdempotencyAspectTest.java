package com.tchalanet.server.platform.idempotence.internal.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.constant.TchHeaders;
import com.tchalanet.server.common.types.enums.IdempotencyScope;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.platform.idempotence.api.IdempotencyStore;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import com.tchalanet.server.platform.idempotence.internal.service.RequestHasher;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

class RequireIdempotencyAspectTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @AfterEach
  void clearRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void missingIdempotencyKeyIsRejectedBeforeProceeding() throws Throwable {
    var store = mock(IdempotencyStore.class);
    var pjp = joinPoint(Map.of("amount", 10));
    bindRequest(null);

    var aspect = new RequireIdempotencyAspect(store, jsonUtils);

    assertThatThrownBy(() -> aspect.around(pjp))
        .isInstanceOf(ProblemRestException.class)
        .hasMessage("idempotency.missing");
    verify(pjp, never()).proceed();
    verify(store, never()).begin(any(), any(), any(), anyLong());
  }

  @Test
  void payloadMismatchIsRejected() throws Throwable {
    var store = mock(IdempotencyStore.class);
    when(store.begin(eq(IdempotencyScope.SALES_SELL_TICKET), eq("key-1"), any(), eq(300L)))
        .thenReturn(new IdempotencyStore.BeginResult(
            IdempotencyStore.Decision.PAYLOAD_MISMATCH, Optional.empty()));
    var pjp = joinPoint(Map.of("amount", 10));
    bindRequest("key-1");

    var aspect = new RequireIdempotencyAspect(store, jsonUtils);

    assertThatThrownBy(() -> aspect.around(pjp))
        .isInstanceOf(ProblemRestException.class)
        .hasMessage("idempotency.payload_mismatch");
    verify(pjp, never()).proceed();
  }

  @Test
  void inProgressDuplicateIsRejected() throws Throwable {
    var store = mock(IdempotencyStore.class);
    when(store.begin(eq(IdempotencyScope.SALES_SELL_TICKET), eq("key-1"), any(), eq(300L)))
        .thenReturn(new IdempotencyStore.BeginResult(
            IdempotencyStore.Decision.IN_PROGRESS, Optional.empty()));
    var pjp = joinPoint(Map.of("amount", 10));
    bindRequest("key-1");

    var aspect = new RequireIdempotencyAspect(store, jsonUtils);

    assertThatThrownBy(() -> aspect.around(pjp))
        .isInstanceOf(ProblemRestException.class)
        .hasMessage("idempotency.in_progress");
    verify(pjp, never()).proceed();
  }

  @Test
  void completedReplayReturnsStoredResponseWithoutProceeding() throws Throwable {
    var store = mock(IdempotencyStore.class);
    when(store.begin(eq(IdempotencyScope.SALES_SELL_TICKET), eq("key-1"), any(), eq(300L)))
        .thenReturn(new IdempotencyStore.BeginResult(
            IdempotencyStore.Decision.ALREADY_COMPLETED,
            Optional.of(new IdempotencyStore.CompletedRecord(null, "{\"ticketId\":\"T-1\"}"))));
    var pjp = joinPoint(Map.of("amount", 10));
    bindRequest("key-1");

    var aspect = new RequireIdempotencyAspect(store, jsonUtils);

    var result = aspect.around(pjp);

    assertThat(result).isEqualTo(Map.of("ticketId", "T-1"));
    verify(pjp, never()).proceed();
  }

  @Test
  void startedRequestCompletesRecordAfterProceeding() throws Throwable {
    var store = mock(IdempotencyStore.class);
    var body = Map.of("amount", 10);
    var hash = RequestHasher.sha256Normalized(jsonUtils, body);
    when(store.begin(IdempotencyScope.SALES_SELL_TICKET, "key-1", hash, 300L))
        .thenReturn(new IdempotencyStore.BeginResult(
            IdempotencyStore.Decision.STARTED, Optional.empty()));
    var pjp = joinPoint(body);
    when(pjp.proceed()).thenReturn(Map.of("ticketId", "T-1"));
    bindRequest("key-1");

    var aspect = new RequireIdempotencyAspect(store, jsonUtils);

    var result = aspect.around(pjp);

    assertThat(result).isEqualTo(Map.of("ticketId", "T-1"));
    verify(store).complete(eq(IdempotencyScope.SALES_SELL_TICKET), eq("key-1"), eq(hash), isNull(), any());
  }

  @Test
  void failedRequestMarksRecordFailed() throws Throwable {
    var store = mock(IdempotencyStore.class);
    var body = Map.of("amount", 10);
    var hash = RequestHasher.sha256Normalized(jsonUtils, body);
    when(store.begin(IdempotencyScope.SALES_SELL_TICKET, "key-1", hash, 300L))
        .thenReturn(new IdempotencyStore.BeginResult(
            IdempotencyStore.Decision.STARTED, Optional.empty()));
    var pjp = joinPoint(body);
    when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));
    bindRequest("key-1");

    var aspect = new RequireIdempotencyAspect(store, jsonUtils);

    assertThatThrownBy(() -> aspect.around(pjp))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");
    verify(store).fail(IdempotencyScope.SALES_SELL_TICKET, "key-1", hash);
  }

  private static ProceedingJoinPoint joinPoint(Object body) throws NoSuchMethodException {
    Method method = DummyController.class.getDeclaredMethod("sell", Object.class);
    var signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);

    var pjp = mock(ProceedingJoinPoint.class);
    when(pjp.getSignature()).thenReturn(signature);
    when(pjp.getTarget()).thenReturn(new DummyController());
    when(pjp.getArgs()).thenReturn(new Object[] {body});
    return pjp;
  }

  private static void bindRequest(String idempotencyKey) {
    var request = new MockHttpServletRequest();
    if (idempotencyKey != null) {
      request.addHeader(TchHeaders.IDEMPOTENCY_KEY, idempotencyKey);
    }
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private static final class DummyController {
    @RequireIdempotency(scope = IdempotencyScope.SALES_SELL_TICKET)
    Object sell(Object body) {
      return body;
    }
  }
}
