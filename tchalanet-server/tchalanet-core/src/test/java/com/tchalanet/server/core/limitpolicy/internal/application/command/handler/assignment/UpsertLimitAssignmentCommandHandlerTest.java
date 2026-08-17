package com.tchalanet.server.core.limitpolicy.internal.application.command.handler.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.command.UpsertLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentWriterPort;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class UpsertLimitAssignmentCommandHandlerTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final LimitAssignmentReaderPort reader = mock(LimitAssignmentReaderPort.class);
  private final LimitAssignmentWriterPort writer = mock(LimitAssignmentWriterPort.class);
  private final IdGenerator idGenerator = mock(IdGenerator.class);

  private final UpsertLimitAssignmentCommandHandler handler =
      new UpsertLimitAssignmentCommandHandler(reader, writer, idGenerator);

  private static final UUID NEW_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
  private static final LimitScopeRef SCOPE = LimitScopeRef.tenant(TENANT);

  @Test
  void creates_new_assignment_when_none_exists() {
    when(reader.findByNaturalKey(any(), any())).thenReturn(Optional.empty());
    when(idGenerator.newUuid()).thenReturn(NEW_ID);

    var savedId = LimitAssignmentId.of(NEW_ID);
    var saved =
        LimitAssignment.createNew(
            savedId,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            true,
            BreachOutcome.BLOCK,
            params(),
            null,
            null);
    when(writer.save(any())).thenReturn(saved);

    var result = handler.handle(validCommand());

    assertThat(result.id()).isEqualTo(savedId);
    verify(idGenerator).newUuid();
    verify(writer).save(any());
  }

  @Test
  void updates_existing_assignment_when_natural_key_matches() {
    var existingId = LimitAssignmentId.of(UUID.randomUUID());
    var existing =
        LimitAssignment.createNew(
            existingId,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            false,
            BreachOutcome.WARN,
            params(),
            null,
            null);

    when(reader.findByNaturalKey(any(), any())).thenReturn(Optional.of(existing));
    when(writer.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var result = handler.handle(validCommand());

    assertThat(result.id()).isEqualTo(existingId);
    verify(idGenerator, never()).newUuid();
    verify(writer).save(any());
  }

  // --- validation ---

  @Test
  void throws_when_tenantId_is_null() {
    var cmd =
        new UpsertLimitAssignmentCommand(
            null,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            true,
            BreachOutcome.BLOCK,
            params(),
            null,
            null);
    assertThatThrownBy(() -> handler.handle(cmd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tenantId");
  }

  @Test
  void throws_when_ruleKey_is_null() {
    var cmd =
        new UpsertLimitAssignmentCommand(
            TENANT, null, SCOPE, true, BreachOutcome.BLOCK, params(), null, null);
    assertThatThrownBy(() -> handler.handle(cmd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ruleKey");
  }

  @Test
  void throws_when_target_is_null() {
    var cmd =
        new UpsertLimitAssignmentCommand(
            TENANT,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            null,
            true,
            BreachOutcome.BLOCK,
            params(),
            null,
            null);
    assertThatThrownBy(() -> handler.handle(cmd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("limitScopeRef");
  }

  @Test
  void throws_when_onBreach_is_null() {
    var cmd =
        new UpsertLimitAssignmentCommand(
            TENANT, RuleKey.BLOCK_SELECTION_PER_DRAW, SCOPE, true, null, params(), null, null);
    assertThatThrownBy(() -> handler.handle(cmd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("onBreach");
  }

  @Test
  void throws_when_params_is_null() {
    var cmd =
        new UpsertLimitAssignmentCommand(
            TENANT,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            true,
            BreachOutcome.BLOCK,
            null,
            null,
            null);
    assertThatThrownBy(() -> handler.handle(cmd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("params");
  }

  @Test
  void throws_when_startsAt_is_not_before_endsAt() {
    var now = Instant.parse("2026-08-12T10:00:00Z");
    var cmd =
        new UpsertLimitAssignmentCommand(
            TENANT,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            true,
            BreachOutcome.BLOCK,
            params(),
            now,
            now); // startsAt == endsAt → invalid
    assertThatThrownBy(() -> handler.handle(cmd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("startsAt must be before endsAt");
  }

  @Test
  void accepts_null_start_and_end() {
    when(reader.findByNaturalKey(any(), any())).thenReturn(Optional.empty());
    when(idGenerator.newUuid()).thenReturn(NEW_ID);
    var saved =
        LimitAssignment.createNew(
            LimitAssignmentId.of(NEW_ID),
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            true,
            BreachOutcome.BLOCK,
            params(),
            null,
            null);
    when(writer.save(any())).thenReturn(saved);

    var cmd =
        new UpsertLimitAssignmentCommand(
            TENANT,
            RuleKey.BLOCK_SELECTION_PER_DRAW,
            SCOPE,
            true,
            BreachOutcome.BLOCK,
            params(),
            null,
            null);

    assertThat(handler.handle(cmd).id()).isNotNull();
  }

  private UpsertLimitAssignmentCommand validCommand() {
    var start = Instant.parse("2026-01-01T00:00:00Z");
    var end = Instant.parse("2026-12-31T23:59:59Z");
    return new UpsertLimitAssignmentCommand(
        TENANT,
        RuleKey.BLOCK_SELECTION_PER_DRAW,
        SCOPE,
        true,
        BreachOutcome.BLOCK,
        params(),
        start,
        end);
  }

  private static tools.jackson.databind.JsonNode params() {
    return MAPPER.createObjectNode().put("valueCents", 10000);
  }
}
