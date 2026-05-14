package com.tchalanet.server.platform.document.api.model;

public sealed interface DocumentContent
    permits ReceiptDocumentContent, ReportDocumentContent, GenericDocumentContent {}
