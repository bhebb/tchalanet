package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.address.domain.Address;

import java.time.LocalTime;

/**
 * Partial config update for an Outlet. Each nullable field means "leave unchanged" when null.
 *
 * <p>Boolean wrappers (vs primitive) are required to differentiate "not provided" from "set to
 * false".
 */
public record OutletConfigPatch(
    Boolean salesBlocked,
    String salesBlockReason,
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
    Address address
) {}
