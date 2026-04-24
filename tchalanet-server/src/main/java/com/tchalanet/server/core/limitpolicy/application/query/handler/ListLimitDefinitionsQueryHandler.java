package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListLimitDefinitionsQueryHandler
    implements QueryHandler<com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsQuery, ListLimitDefinitionsView> {

    private final LimitDefinitionReaderPort reader;

    @Override
    public ListLimitDefinitionsView handle(com.tchalanet.server.core.limitpolicy.application.query.model.ListLimitDefinitionsQuery q) {
        var items =
            reader.listActive().stream()
                .filter(d -> !d.isDeleted())
                .map(d -> new ListLimitDefinitionsView.Item(
                    d.id(),
                    d.ruleKey(),
                    d.enabled(),
                    d.onBreach(),
                    d.params(),
                    d.appliesTo()
                ))
                .toList();

        return new ListLimitDefinitionsView(items);
    }
}
