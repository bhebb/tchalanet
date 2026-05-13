package com.tchalanet.server.platform.document.api;

import java.util.List;

public interface DocumentApi {

  byte[] renderReceiptPdf(String title, List<String> bodyLines, byte[] qrPng);

  byte[] renderReceiptEscPos(String title, List<String> bodyLines, byte[] qrEscPos);

  byte[] renderQrPng(String payload, int sizePx);

  byte[] renderQrEscPos(String payload, int sizePx);
}
