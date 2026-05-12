package com.tchalanet.server.core.audit.infra.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.aspectj.runtime.internal.AroundClosure;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

class AuditLogAspectTest {

  @Test
  void successAuditCanUseResultAndMethodParametersInSpel() throws Throwable {
    var commands = new ArrayList<LogAuditEventCommand>();
    var aspect = new AuditLogAspect(recordingBus(commands), jsonUtils());
    var pjp =
        joinPoint(
            "sell",
            new String[] {"ticketCode"},
            new Object[] {"TCK-123"},
            () -> new TicketResult("PUBLIC-ABC", 2500));

    Object result = aspect.aroundAudit(pjp);

    assertThat(result).isEqualTo(new TicketResult("PUBLIC-ABC", 2500));
    assertThat(commands).hasSize(1);
    var cmd = commands.getFirst();
    assertThat(cmd.entityType()).isEqualTo(AuditEntityType.TICKET);
    assertThat(cmd.action()).isEqualTo(AuditAction.CREATE);
    assertThat(cmd.entityId()).isEqualTo("PUBLIC-ABC");
    assertThat(cmd.details())
        .containsEntry("input", "TCK-123")
        .containsEntry("amount", 2500)
        .containsEntry("outcome", "SUCCESS");
  }

  @Test
  void errorAuditCanUseErrorAndPreservesOriginalException() throws Throwable {
    var commands = new ArrayList<LogAuditEventCommand>();
    var aspect = new AuditLogAspect(recordingBus(commands), jsonUtils());
    var pjp =
        joinPoint(
            "fail",
            new String[] {"ticketCode"},
            new Object[] {"TCK-ERR"},
            () -> {
              throw new IllegalStateException("boom");
            });

    assertThatThrownBy(() -> aspect.aroundAudit(pjp))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(commands).hasSize(1);
    var cmd = commands.getFirst();
    assertThat(cmd.entityId()).isEqualTo("TCK-ERR");
    assertThat(cmd.details())
        .containsEntry("input", "TCK-ERR")
        .containsEntry("message", "boom")
        .containsEntry("error", "IllegalStateException")
        .containsEntry("errorMessage", "boom")
        .containsEntry("outcome", "FAIL");
  }

  @Test
  void nonUuidEntityIdIsAccepted() throws Throwable {
    var commands = new ArrayList<LogAuditEventCommand>();
    var aspect = new AuditLogAspect(recordingBus(commands), jsonUtils());
    var pjp =
        joinPoint("print", new String[] {"publicCode"}, new Object[] {"PUB-CODE-42"}, () -> "ok");

    aspect.aroundAudit(pjp);

    assertThat(commands).extracting(LogAuditEventCommand::entityId).containsExactly("PUB-CODE-42");
  }

  @Test
  void detailsIncludeRequestIdWhenContextProvidesOne() throws Throwable {
    var commands = new ArrayList<LogAuditEventCommand>();
    var aspect = new AuditLogAspect(recordingBus(commands), jsonUtils(), new TchContextResolver());
    var pjp =
        joinPoint("print", new String[] {"publicCode"}, new Object[] {"PUB-CODE-42"}, () -> "ok");

    TchContext.set(TchRequestContext.startupTenant(UUID.randomUUID(), "req-audit-123"));
    try {
      aspect.aroundAudit(pjp);
    } finally {
      TchContext.clear();
    }

    assertThat(commands).hasSize(1);
    assertThat(commands.getFirst().details()).containsEntry("requestId", "req-audit-123");
  }

  @Test
  void auditCommandFailureDoesNotBreakBusinessSuccess() throws Throwable {
    var aspect =
        new AuditLogAspect(
            new CommandBus() {
              @Override
              public <R> R execute(Command<R> command) {
                throw new IllegalStateException("audit down");
              }
            },
            jsonUtils());
    var pjp =
        joinPoint("print", new String[] {"publicCode"}, new Object[] {"PUB-CODE-42"}, () -> "ok");

    assertThat(aspect.aroundAudit(pjp)).isEqualTo("ok");
  }

  @Test
  void successAuditIsSentAfterCommitWhenTransactionIsActive() throws Throwable {
    var commands = new ArrayList<LogAuditEventCommand>();
    var aspect = new AuditLogAspect(recordingBus(commands), jsonUtils());
    var pjp =
        joinPoint("print", new String[] {"publicCode"}, new Object[] {"PUB-CODE-42"}, () -> "ok");

    beginTransactionSynchronization();
    try {
      aspect.aroundAudit(pjp);

      assertThat(commands).isEmpty();

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(synchronization -> synchronization.afterCommit());

      assertThat(commands).hasSize(1);
    } finally {
      clearTransactionSynchronization();
    }
  }

  @Test
  void successAuditIsNotSentWhenTransactionRollsBack() throws Throwable {
    var commands = new ArrayList<LogAuditEventCommand>();
    var aspect = new AuditLogAspect(recordingBus(commands), jsonUtils());
    var pjp =
        joinPoint("print", new String[] {"publicCode"}, new Object[] {"PUB-CODE-42"}, () -> "ok");

    beginTransactionSynchronization();
    try {
      aspect.aroundAudit(pjp);

      assertThat(commands).isEmpty();
    } finally {
      clearTransactionSynchronization();
    }
  }

  @SuppressWarnings("unchecked")
  private static CommandBus recordingBus(List<LogAuditEventCommand> commands) {
    return new CommandBus() {
      @Override
      public <R> R execute(Command<R> command) {
        commands.add((LogAuditEventCommand) command);
        return null;
      }
    };
  }

  private static JsonUtils jsonUtils() {
    return new JsonUtils(JsonMapper.builder().build());
  }

  private static void beginTransactionSynchronization() {
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
  }

  private static void clearTransactionSynchronization() {
    TransactionSynchronizationManager.setActualTransactionActive(false);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private static ProceedingJoinPoint joinPoint(
      String methodName, String[] paramNames, Object[] args, ProceedAction action)
      throws NoSuchMethodException {
    var fixture = new Fixture();
    Method method =
        switch (methodName) {
          case "sell" -> Fixture.class.getDeclaredMethod(methodName, String.class);
          case "fail" -> Fixture.class.getDeclaredMethod(methodName, String.class);
          case "print" -> Fixture.class.getDeclaredMethod(methodName, String.class);
          default -> throw new IllegalArgumentException(methodName);
        };
    return new FakeProceedingJoinPoint(fixture, args, new FakeMethodSignature(method, paramNames), action);
  }

  @FunctionalInterface
  private interface ProceedAction {
    Object proceed() throws Throwable;
  }

  private record FakeProceedingJoinPoint(
      Object target, Object[] args, MethodSignature signature, ProceedAction action)
      implements ProceedingJoinPoint {

    @Override
    public Object proceed() throws Throwable {
      return action.proceed();
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
      return action.proceed();
    }

    @Override
    public void set$AroundClosure(AroundClosure arc) {}

    @Override
    public String toShortString() {
      return signature.toShortString();
    }

    @Override
    public String toLongString() {
      return signature.toLongString();
    }

    @Override
    public Object getThis() {
      return target;
    }

    @Override
    public Object getTarget() {
      return target;
    }

    @Override
    public Object[] getArgs() {
      return args;
    }

    @Override
    public Signature getSignature() {
      return signature;
    }

    @Override
    public SourceLocation getSourceLocation() {
      return null;
    }

    @Override
    public String getKind() {
      return JoinPoint.METHOD_EXECUTION;
    }

    @Override
    public StaticPart getStaticPart() {
      return null;
    }
  }

  private record FakeMethodSignature(Method method, String[] parameterNames)
      implements MethodSignature {

    @Override
    public Class getReturnType() {
      return method.getReturnType();
    }

    @Override
    public Method getMethod() {
      return method;
    }

    @Override
    public Class[] getParameterTypes() {
      return method.getParameterTypes();
    }

    @Override
    public String[] getParameterNames() {
      return parameterNames;
    }

    @Override
    public Class[] getExceptionTypes() {
      return method.getExceptionTypes();
    }

    @Override
    public String toShortString() {
      return method.getName();
    }

    @Override
    public String toLongString() {
      return method.toGenericString();
    }

    @Override
    public String getName() {
      return method.getName();
    }

    @Override
    public int getModifiers() {
      return method.getModifiers();
    }

    @Override
    public Class getDeclaringType() {
      return method.getDeclaringClass();
    }

    @Override
    public String getDeclaringTypeName() {
      return method.getDeclaringClass().getName();
    }
  }

  private record TicketResult(String publicCode, int amount) {}

  private static class Fixture {
    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.CREATE,
        idExpression = "#result.publicCode",
        detailsExpression = "{'input': #ticketCode, 'amount': #result.amount}")
    TicketResult sell(String ticketCode) {
      return null;
    }

    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.OTHER,
        idExpression = "#ticketCode",
        detailsExpression = "{'input': #ticketCode, 'message': #error.message}")
    TicketResult fail(String ticketCode) {
      return null;
    }

    @AuditLog(
        entity = AuditEntityType.TICKET,
        action = AuditAction.OTHER,
        idExpression = "#publicCode")
    String print(String publicCode) {
      return "ok";
    }
  }
}
