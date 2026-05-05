package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.draw.application.query.model.DrawSearchCriteria;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Port de lecture standardisé pour les vues {@link DrawSummary}.
 */
public interface DrawSummaryReaderPort {

    /**
     * Récupère un draw summary par son identifiant.
     */
    Optional<DrawSummary> findById(DrawId drawId);

    /**
     * Récupère un draw summary par son identifiant.
     */
    DrawSummary getById(DrawId drawId);

    /**
     * Liste les draws selon les critères de recherche.
     */
    TchPage<DrawSummary> findByCriteria(DrawSearchCriteria criteria, Pageable pageable);

    /**
     * Liste les prochains draws (SCHEDULED, OPEN, sans résultat).
     */
    TchPage<DrawSummary> listNext(DrawSearchCriteria criteria, Pageable pageable);

    /**
     * Liste les derniers draws avec résultats appliqués.
     */
    TchPage<DrawSummary> listLatestWithResults(DrawSearchCriteria criteria, Pageable pageable);
}
