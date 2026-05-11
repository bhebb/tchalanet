package com.tchalanet.server.core.offlinesync.domain.service;

public class OfflineSignaturePolicy {

  public boolean isPayloadHashValid(String payloadJson, String payloadHash) {
    return payloadJson != null && !payloadJson.isBlank() && payloadHash != null && !payloadHash.isBlank();
  }

  public boolean isSignatureShapeValid(String signature) {
    return signature != null && !signature.isBlank();
  }
}

