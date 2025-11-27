package com.tchalanet.server.draw.domain.ports.out;

import com.tchalanet.server.draw.domain.model.ExternalDrawResult;
import java.util.List;
import java.util.UUID;

/**
 * Outbound Port for fetching external draw results from various providers. This port abstracts the
 * source of external draw results (e.g., US lotteries, HT lotteries).
 */
public interface ExternalDrawResultProviderPort {

  /**
   * Fetches the latest external draw results for a given tenant. The implementation will decide
   * which external APIs to call based on configuration or tenant settings.
   *
   * @param tenantId The ID of the tenant for which to fetch results.
   * @return A list of ExternalDrawResult objects.
   */
  List<ExternalDrawResult> fetchLatestExternalDrawResults(UUID tenantId);
}
