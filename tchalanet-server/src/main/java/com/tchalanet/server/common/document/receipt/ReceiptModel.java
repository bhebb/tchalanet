package com.tchalanet.server.common.document.receipt;

import java.util.List;

public record ReceiptModel(String title, List<ReceiptLine> lines) {}
