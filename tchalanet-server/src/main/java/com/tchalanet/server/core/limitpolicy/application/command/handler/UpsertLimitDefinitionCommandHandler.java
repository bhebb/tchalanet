package com.tchalanet.server.core.limitpolicy.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpsertLimitDefinitionResult;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionWriterPort;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpsertLimitDefinitionCommandHandler
    implements CommandHandler<UpsertLimitDefinitionCommand, UpsertLimitDefinitionResult> {

  private final TchContextResolver ctx;
  private final LimitDefinitionReaderPort reader;
  private final LimitDefinitionWriterPort writer;

  @Override
  @TchTx
  public UpsertLimitDefinitionResult handle(UpsertLimitDefinitionCommand c) {
    TenantId tenantId = TenantId.of(ctx.currentOrThrow().tenantUuid());

    // Upsert by RuleKey (active row only; do not resurrect deleted)
    var existing = reader.findByRuleKey(c.ruleKey());

    LimitDefinition toSave =
        existing
            .map(d -> new LimitDefinition(
                d.id(),
                d.ruleKey(),
                c.enabled(),
                c.onBreach(),
                c.params(),
                c.appliesTo(),
                null // deletedAt stays null
            ))
            .orElseGet(() -> new LimitDefinition(
                null, // id generated in persistence
                c.ruleKey(),
                c.enabled(),
                c.onBreach(),
                c.params(),
                c.appliesTo(),
                null
            ));

    LimitDefinition saved = writer.save(toSave);

    return new UpsertLimitDefinitionResult(saved.id(), saved.ruleKey());
  }
}
