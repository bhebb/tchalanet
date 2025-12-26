package com.tchalanet.server.features.pagemodel.shared.dynamic;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.pagemodel.shared.block.ResultsByGameBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class SharedResultsByGameProvider implements ResultsByGameProvider {

    private final SharedResultsByGameAggregator aggregator;
    private final TchRequestContextHolder tchRequestContextHolder;

    @Override
    public ResultsByGameBlock buildResultsBlock(
        PageModel pageModel,
        String currentLang
    ) {
        // currentLang utile plus tard si tu veux filtrer/ordonner,
        // mais pour l’instant on s’en sert pas.
        return aggregator.buildResultsBlock(tchRequestContextHolder.get().tenantid());
    }
}

