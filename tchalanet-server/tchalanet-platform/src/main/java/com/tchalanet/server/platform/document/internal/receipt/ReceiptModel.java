package com.tchalanet.server.platform.document.internal.receipt;

import java.util.List;

public record ReceiptModel(String title, List<ReceiptLine> lines) {}
