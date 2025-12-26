package com.tchalanet.server.core.outlet.application.command.model;

public record OutletConfigPatch(
    Boolean salesBlocked,
    String salesBlockReason,
    String timezone,
    String businessDayCutoff,
    Boolean receiptPrintingEnabled,
    String receiptHeaderMessage,
    String receiptFooterMessage,
    Boolean requireOpeningFloat
) {}

