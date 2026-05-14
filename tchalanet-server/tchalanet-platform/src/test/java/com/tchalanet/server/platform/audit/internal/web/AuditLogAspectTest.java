package com.tchalanet.server.platform.audit.internal.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.LogAuditEventCommand;
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
    var commandBus = mock(CommandBus.class);
    when(commandBus.execute(any(Command.class))).thenThrow(new IllegalStateException("audit down"));
    var aspect = new AuditLogAspect(commandBus, jsonUtils);
    var pjp = joinPoint("ok", null);

    var result = aspect.aroundAudit(pjp);

    assertThat(result).isEqualTo("ok");
    verify(commandBus).execute(any(LogAuditEventCommand.class));
  }

  @Test
  void failureAuditIsRecordedAndOriginalFailureIsRethrown() throws Throwable {
    var commandBus = mock(CommandBus.class);
    var aspect = new AuditLogAspect(commandBus, jsonUtils);
    var failure = new IllegalArgumentException("bad write");
    var pjp = joinPoint(null, failure);

    assertThatThrownBy(() -> aspect.aroundAudit(pjp))
        .isSameAs(failure);

    var captor = ArgumentCaptor.forClass(LogAuditEventCommand.class);
    verify(commandBus).execute(captor.capture());
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
