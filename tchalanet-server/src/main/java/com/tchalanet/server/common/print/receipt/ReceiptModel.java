package com.tchalanet.server.common.print.receipt;

import java.util.List;

public record ReceiptModel(String title, List<ReceiptLine> lines) {}
