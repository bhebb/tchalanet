package com.tchalanet.server.platform.notification.internal.rule;

import com.tchalanet.server.common.job.lifecycle.JobLifecycleEvent;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleStatus;
import com.tchalanet.server.platform.notification.api.model.NotificationCategory;
import com.tchalanet.server.platform.notification.api.model.NotificationKind;
import com.tchalanet.server.platform.notification.api.model.NotificationSeverity;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class BatchAlertNotificationRule extends AbstractNotificationRule {

  private static final Set<String> QUIET_SKIP_CODES = Set.of(
      "scheduler_disabled",
      "generate_disabled",
      "open_today_disabled",
      "processing_disabled",
      "watchdog_disabled",
      "gate_disabled",
      "no_active_tenants");

  @Override
  public String handlerKey() {
    return "notification.ops.job_lifecycle";
  }

  @Override
  public boolean supports(Object event) {
    return event instanceof JobLifecycleEvent lifecycle && shouldNotify(lifecycle);
  }

  @Override
  public Stream<NotificationIntent> map(Object event) {
    var lifecycle = (JobLifecycleEvent) event;
    var failed = lifecycle.status() == JobLifecycleStatus.FAILED;
    var templateKey = failed ? "notification.system.ops.job_failed" : "notification.system.ops.job_skipped";
    var severity = failed ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING;
    var kind = failed ? NotificationKind.SYSTEM_ERROR : NotificationKind.WARNING;

    return Stream.of(platformIntent(
        lifecycle,
        templateKey,
        severity,
        kind,
        NotificationCategory.BATCH,
        title(lifecycle),
        message(lifecycle)));
  }

  private boolean shouldNotify(JobLifecycleEvent event) {
    if (event.status() == JobLifecycleStatus.FAILED) {
      return true;
    }

    if (event.status() == JobLifecycleStatus.SKIPPED) {
      return event.code() != null && !QUIET_SKIP_CODES.contains(event.code());
    }

    return false;
  }

  private String title(JobLifecycleEvent event) {
    if (event.status() == JobLifecycleStatus.FAILED) {
      return "Job failed: " + event.jobKey();
    }
    return "Job skipped: " + event.jobKey();
  }

  private String message(JobLifecycleEvent event) {
    var builder = new StringBuilder();
    builder.append("Job ").append(event.jobKey()).append(" ended with status ").append(event.status());
    if (event.code() != null && !event.code().isBlank()) {
      builder.append(" (").append(event.code()).append(")");
    }
    if (event.message() != null && !event.message().isBlank()) {
      builder.append(": ").append(event.message());
    }
    return builder.toString();
  }
}
