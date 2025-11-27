package com.tchalanet.server.uslottery.domain.ports.in;

/** Use case pour rafraîchir les résultats US Lottery (NY, Florida...). */
public interface RefreshUsLotteryResultsUseCase {

  void refresh();
}
