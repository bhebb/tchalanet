package com.tchalanet.server.core.uslottery.application.port.out;

import com.tchalanet.server.common.types.enums.UsLotteryProvider;

/**
 * Port implemented by provider adapters that fetch draws from US providers (NY/FL/GA/etc.).
 *
 * <p>Adapters should implement this interface and return provider-specific draws for the requested
 * date/window. The caller is responsible for looping daysBack if necessary.
 */
public interface UsLotteryProviderClient {

    /**
     * Provider identifier (NY, FL, GA, ...)
     */
    UsLotteryProvider provider();

    /**
     * Fetch draws according to the provided query. Prefer fetching for a single date per call. The
     * caller (orchestrator) may call this for several dates when daysBack & maxDraws are used.
     * externalGameCodes may be empty — adapter should return whatever draws are available for that date.
     */
    UsLotteryProviderResponse fetch(UsLotteryProviderQuery query);
}
