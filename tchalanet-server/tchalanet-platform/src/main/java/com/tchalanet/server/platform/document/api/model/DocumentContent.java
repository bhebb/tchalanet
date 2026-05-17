package com.tchalanet.server.platform.document.api.model;

public sealed interface DocumentContent
    permits GenericDocumentContent, QrDocumentContent, ReceiptDocumentContent, ReportDocumentContent {}
