package com.tchalanet.server.platform.document.api.model;

public enum DocumentKind {
    RECEIPT,
    REPORT,
    QR_CODE;

    public boolean supports(DocumentContent content) {
        return switch (this) {
            case RECEIPT -> content instanceof ReceiptDocumentContent;
            case REPORT -> content instanceof ReportDocumentContent;
            case QR_CODE -> content instanceof QrDocumentContent;
        };
    }
}
