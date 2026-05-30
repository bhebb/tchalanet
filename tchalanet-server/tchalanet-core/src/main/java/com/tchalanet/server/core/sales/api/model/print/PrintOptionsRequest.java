package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.platform.document.api.model.PaperSize;

/**
 * Small optional object sent by clients to indicate desired generation output.
 * Defaults are handled by a resolver: outputFormat=PDF, paperSize=A4 when absent.
 */
public record PrintOptionsRequest(
    PrintOutputFormat outputFormat,
    PaperSize paperSize
) {
}

