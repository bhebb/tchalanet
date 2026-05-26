package com.tchalanet.server.core.outlet.api.command;

import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.platform.address.api.model.AddressInput;

/**
 * Partial config update for an Outlet. Each nullable field means "leave unchanged" when null.
 *
 * <p>Boolean wrappers (vs primitive) are required to differentiate "not provided" from "set to
 * false".
 *
 * <p>Note: {@code kind} is intentionally absent — OutletKind is immutable after creation.
 */
public record OutletConfigPatch(
    String partnerRef,
    SalesZoneId zoneId,
    String timezone,
    Boolean receiptPrintingEnabled,
    String receiptHeaderMessage,
    String receiptFooterMessage,
    Boolean requireOpeningFloat,
    Boolean autoSessionOpenEnabled,
    Boolean autoSessionCloseEnabled,
    String sessionOpenTime,
    String sessionCloseTime,
    Long defaultOpeningFloatCents,
    AddressInput address
) {}
