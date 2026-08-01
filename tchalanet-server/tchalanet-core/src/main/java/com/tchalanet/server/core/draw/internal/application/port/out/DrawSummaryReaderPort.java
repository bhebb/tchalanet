package com.tchalanet.server.core.draw.internal.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.api.query.DrawResultAffectedTenant;
import com.tchalanet.server.core.draw.api.query.DrawSearchCriteria;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.DrawsSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

/** Port de lecture standardisé pour les vues {@link DrawSummary}. */
public interface DrawSummaryReaderPort {

  /** Récupère un draw summary par son identifiant. */
  Optional<DrawSummary> findById(DrawId drawId);

  /** Récupère un draw summary par son identifiant. */
  DrawSummary getById(DrawId drawId);

  /** Liste les draws selon les critères de recherche. */
  TchPage<DrawSummary> findByCriteria(DrawSearchCriteria criteria, Pageable pageable);

  /** Returns exact KPI counts for the requested business-date range. */
  DrawsSummary summarize(DrawSearchCriteria criteria, LocalDate today, Instant now);

  /** Liste les prochains draws (SCHEDULED, OPEN, sans résultat). */
  TchPage<DrawSummary> listNext(DrawSearchCriteria criteria, Pageable pageable);

  /** Liste les derniers draws avec résultats appliqués. */
  TchPage<DrawSummary> listLatestWithResults(DrawSearchCriteria criteria, Pageable pageable);

  /** Liste les tenants qui ont un draw correspondant, actif et applicable à un résultat global. */
  List<DrawResultAffectedTenant> listAffectedTenants(ResultSlotId resultSlotId, LocalDate drawDate);
}
