package com.tchalanet.server.core.offlinesync.application.service;

import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineCryptoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfflineTechnicalValidator {

  private final OfflineCryptoPort cryptoPort;

  public boolean isValid(TerminalId terminalId, String payloadJson, String payloadHash, String signature) {
    return cryptoPort.verifyPayloadHash(payloadJson, payloadHash)
        && cryptoPort.verifyPayloadSignature(terminalId, payloadJson, payloadHash, signature);
  }
}

