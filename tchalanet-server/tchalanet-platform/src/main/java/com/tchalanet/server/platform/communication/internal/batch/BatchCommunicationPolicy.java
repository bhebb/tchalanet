package com.tchalanet.server.platform.communication.internal.batch;

import com.tchalanet.server.common.job.lifecycle.JobLifecycleEvent;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleStatus;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class BatchCommunicationPolicy {

  private static final Set<String> QUIET_SKIP_CODES = Set.of(
      "scheduler_disabled",
      "generate_disabled",
      "open_today_disabled",
      "processing_disabled",
      "watchdog_disabled",
      "gate_disabled",
      "no_active_tenants");

  public boolean shouldSend(JobLifecycleEvent event) {
    if (event.status() == JobLifecycleStatus.FAILED) {
      return true;
    }

    if (event.status() == JobLifecycleStatus.SKIPPED) {
      return event.code() != null && !QUIET_SKIP_CODES.contains(event.code());
    }

    return false;
  }
}
