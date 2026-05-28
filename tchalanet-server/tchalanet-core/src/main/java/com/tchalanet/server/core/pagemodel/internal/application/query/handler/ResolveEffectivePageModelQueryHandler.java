package com.tchalanet.server.core.pagemodel.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.TchContextScope;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.core.pagemodel.internal.application.port.out.PageModelReadPort;
import com.tchalanet.server.core.pagemodel.internal.domain.model.PageModelInstance;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.tchalanet.server.common.constant.CommonConstants.DEFAULT_TENANT_UUID;

// [Phase 3A] @UseCase + QueryHandler pour câblage CQRS (analysis §MAJEUR command_query_handlers.md §4.2)
// [Phase 3A] new ObjectMapper() → JsonUtils injecté (analysis §MAJEUR)
@UseCase
@RequiredArgsConstructor
public class ResolveEffectivePageModelQueryHandler
    implements QueryHandler<ResolveEffectivePageModelQuery, PageModelDoc> {

    private final PageModelReadPort readPort;
    private final JsonUtils jsonUtils;

    @Override
    public PageModelDoc handle(ResolveEffectivePageModelQuery q) {
        Optional<PageModelDoc> tenantDoc =
            q.tenantId()
                .flatMap(tenantId -> readPort.findPublishedByLogicalId(q.logicalId()).map(this::toDoc));

        if (tenantDoc.isPresent()) {
            return tenantDoc.get();
        }

        boolean alreadyDefaultTenant =
            q.tenantId()
                .map(TenantId::value)
                .filter(DEFAULT_TENANT_UUID::equals)
                .isPresent();

        if (alreadyDefaultTenant) {
            throw new TchNotFoundException(
                "PAGE_MODEL_NOT_FOUND",
                "Page model not found: " + q.logicalId());
        }

        Optional<PageModelDoc> defaultDoc =
            TchContextScope.runWithTemporaryTenantResult(
                DEFAULT_TENANT_UUID,
                "pagemodel:resolve-default",
                () -> readPort.findPublishedByLogicalId(q.logicalId()).map(this::toDoc));

        return defaultDoc.orElseThrow(() ->
            new TchNotFoundException(
                "PAGE_MODEL_NOT_FOUND",
                "Page model not found: " + q.logicalId()));
    }

    private PageModelDoc toDoc(PageModelInstance inst) {
        if (inst == null || inst.modelJson() == null) {
            throw new TchNotFoundException(
                "PAGE_MODEL_EMPTY",
                "Page model has empty modelJson");
        }

        try {
            return jsonUtils.treeToValue(inst.modelJson(), PageModelDoc.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PageModelInstance.modelJson to PageModelDoc", e);
        }
    }
}
