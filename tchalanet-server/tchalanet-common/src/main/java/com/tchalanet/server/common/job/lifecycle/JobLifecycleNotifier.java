package com.tchalanet.server.common.job.lifecycle;

public interface JobLifecycleNotifier {

  void started(String jobKey);

  void succeeded(String jobKey);

  void skipped(String jobKey, String code, String message);

  void failed(String jobKey, Throwable error);
}
