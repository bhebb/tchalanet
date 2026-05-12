package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.TerminalId;

public interface OfflineCryptoPort {
  boolean verifyPayloadSignature(TerminalId terminalId, String payloadJson, String payloadHash, String signature);
  boolean verifyPayloadHash(String payloadJson, String payloadHash);
}
