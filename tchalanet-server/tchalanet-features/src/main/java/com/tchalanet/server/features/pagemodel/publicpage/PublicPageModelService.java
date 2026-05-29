package com.tchalanet.server.features.pagemodel.publicpage;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicResolver;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Public PageModel resolver.
 * <p>
 * Public rule:
 * - Resolves public PageModels only.
 * - Returns 404 for private/non-public documents.
 * - Does not use authenticated admin/cashier tenant context as public tenant source.
 */
@Service
@RequiredArgsConstructor
public class PublicPageModelService {

    private final QueryBus queryBus;
    private final TchContextResolver contextResolver;
    private final LangResolver langResolver;
    private final PageModelDynamicResolver dynamicResolver;

    public PublicPageModelResponse resolve(String logicalId, Optional<String> langFromUrl) {
        var ctxHolder = contextResolver.currentOrNull();

        PageModelDoc doc = queryBus.ask(new ResolveEffectivePageModelQuery(Optional.empty(), logicalId));

        if (doc == null || doc.meta() == null || !"public".equals(doc.meta().scope())) {
            throw new TchNotFoundException(
                "PAGE_MODEL_NOT_FOUND",
                "Page model not found: " + logicalId);
        }

        var currentLang = resolveLang(doc, langFromUrl);
        var dynamic = dynamicResolver.resolve(doc, currentLang, ctxHolder);
        var langs = langsOrCurrent(doc, currentLang);

        return new PublicPageModelResponse(currentLang, langs, doc, dynamic);
    }

    private String resolveLang(PageModelDoc doc, Optional<String> langFromUrl) {
        boolean hasDocLangs = doc.meta() != null
            && doc.meta().langs() != null
            && !doc.meta().langs().isEmpty();

        return langResolver.resolve(
            new LangResolver.LangResolverContext(
                langFromUrl,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(doc.meta() != null ? doc.meta().defaultLang() : null),
                hasDocLangs ? doc.meta().langs() : List.of(),
                "fr"));
    }

    private List<String> langsOrCurrent(PageModelDoc doc, String currentLang) {
        boolean hasDocLangs = doc.meta() != null
            && doc.meta().langs() != null
            && !doc.meta().langs().isEmpty();

        return hasDocLangs ? doc.meta().langs() : List.of(currentLang);
    }
}
