package com.tchalanet.server.platform.communication.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.communication.api.model.request.CommunicationFee;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;

import java.util.Optional;

/**
 * Public read API for tenant-level notification fee configuration.
 *
 * <p>Returns the fee policy for a (tenant, channel) pair, or empty when no
 * fee is configured (in which case the channel is free for the buyer and the
 * tenant absorbs the cost implicitly).
 *
 * <p>This API is consumed by {@code core.sales} during ticket sale preparation
 * to convert seller-selected notification options into {@code TicketCharge}s.
 * It is NOT consumed for actually sending notifications — that flow lives
 * elsewhere in {@code platform.notification}.
 */
public interface CommunicationFeePolicy {

    /**
     * @return the configured fee for the given channel, or empty if the channel
     * is not enabled or not chargeable for this tenant.
     */
    Optional<CommunicationFee> feeFor(TenantId tenantId, CommunicationChannel channel);
}
