package com.tchalanet.server.core.offlinesync.internal.domain.service;

public class OfflineSequencePolicy {

  public boolean isNextExpected(long lastAcceptedSequence, long incomingSequence) {
    return incomingSequence == lastAcceptedSequence + 1;
  }

  public boolean isDuplicate(long lastAcceptedSequence, long incomingSequence) {
    return incomingSequence <= lastAcceptedSequence;
  }
}

