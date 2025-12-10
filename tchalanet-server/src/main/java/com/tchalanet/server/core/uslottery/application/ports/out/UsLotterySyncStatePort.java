package com.tchalanet.server.core.uslottery.application.ports.out;

import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;

/**
 * Port pour gérer l'état de synchronisation des tirages US Lottery (NY/FL) côté stockage
 * (DB/Redis). Permet d'éviter des appels API inutiles à un niveau par tirage.
 */
public interface UsLotterySyncStatePort {

  /** Indique s'il est pertinent de tenter un fetch API pour ce tirage (provider+channel+date). */
  boolean shouldFetch(LatestDraw probe);

  /** Marque qu'un fetch vient d'être effectué pour ce tirage. */
  void markFetchAttempt(LatestDraw probe);
}
