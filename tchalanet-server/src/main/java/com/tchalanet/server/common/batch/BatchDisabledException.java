package com.tchalanet.server.common.batch;

public class BatchDisabledException extends RuntimeException {
  private final String jobKey;

  public BatchDisabledException(String jobKey) {
    super("Batch job disabled: " + jobKey);
    this.jobKey = jobKey;
  }

  public String jobKey() {
    return jobKey;
  }
}
