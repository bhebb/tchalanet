package com.tchalanet.server.platform.audit.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.audit.api.AuditApi;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class AuditLogAspectTest {

  private final JsonUtils jsonUtils = new JsonUtils(JsonMapper.builder().build());

  @Test
  void successAuditFailureDoesNotBreakBusinessResult() throws Throwable {
    var auditApi = mock(AuditApi.class);
    doThrow(new IllegalStateException("audit down"))
        .when(auditApi)
        .logAuditEvent(any(LogAuditEventRequest.class));
    var aspect = new AuditLogAspect(auditApi, jsonUtils);
    var pjp = joinPoint("ok", null);

    var result = aspect.aroundAudit(pjp);

    assertThat(result).isEqualTo("ok");
    verify(auditApi).logAuditEvent(any(LogAuditEventRequest.class));
  }

  @Test
  void failureAuditIsRecordedAndOriginalFailureIsRethrown() throws Throwable {
    var auditApi = mock(AuditApi.class);
    var aspect = new AuditLogAspect(auditApi, jsonUtils);
    var failure = new IllegalArgumentException("bad write");
    var pjp = joinPoint(null, failure);

    assertThatThrownBy(() -> aspect.aroundAudit(pjp))
        .isSameAs(failure);

    var captor = ArgumentCaptor.forClass(LogAuditEventRequest.class);
    verify(auditApi).logAuditEvent(captor.capture());
    assertThat(captor.getValue().details())
        .containsEntry("outcome", "FAIL")
        .containsEntry("error", "IllegalArgumentException")
        .containsEntry("errorMessage", "bad write");
  }

  private static ProceedingJoinPoint joinPoint(Object result, Throwable failure)
      throws Throwable {
    Method method = AuditedController.class.getDeclaredMethod("write");
    var signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(method);
    when(signature.getParameterNames()).thenReturn(new String[0]);

    var pjp = mock(ProceedingJoinPoint.class);
    when(pjp.getSignature()).thenReturn(signature);
    when(pjp.getTarget()).thenReturn(new AuditedController());
    when(pjp.getArgs()).thenReturn(new Object[0]);
    if (failure == null) {
      when(pjp.proceed()).thenReturn(result);
    } else {
      when(pjp.proceed()).thenThrow(failure);
    }
    return pjp;
  }

  private static final class AuditedController {
    @AuditLog(action = AuditAction.UPDATE, entity = AuditEntityType.SYSTEM, idExpression = "'role-1'")
    String write() {
      return "ok";
    }
  }
}
