package com.tchalanet.server.platform.document.api.model;

/**
 * Small optional object sent by clients to indicate desired generation output.
 * Defaults are handled by a resolver: outputFormat=PDF, paperSize=A4 when absent.
 */
public record PrintOptionsRequest(
    DocumentFormat outputFormat,
    PaperSize paperSize
) {
}

