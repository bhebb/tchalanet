package com.tchalanet.server.core.pagemodel.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pagemodel.application.port.out.PageModelReaderPort;
import com.tchalanet.server.core.pagemodel.application.query.model.PreviewPageModelQuery;
import com.tchalanet.server.core.pagemodel.infra.web.PageModelAdminMapper;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminDetailDto;
import lombok.RequiredArgsConstructor;

/**
 * Handler de preview admin : retourne le PageModelAdminDetailDto mappé sans résolution
 * dynamique. Pas de fallback tenant → default ; RLS garantit l'appartenance au tenant courant.
 */
@UseCase
@RequiredArgsConstructor
public class PreviewPageModelQueryHandler
    implements QueryHandler<PreviewPageModelQuery, PageModelAdminDetailDto> {

  private final PageModelReaderPort reader;
  private final PageModelAdminMapper mapper;

  @Override
  public PageModelAdminDetailDto handle(PreviewPageModelQuery q) {
    return reader.findById(q.id())
        .map(mapper::toAdminDetailDto)
        .orElseThrow(() -> ProblemRest.notFound("pagemodel.not_found", q.id()));
  }
}
