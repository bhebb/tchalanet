package com.tchalanet.server.common.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

public final class JobParamReader {

  private final JobParameters params;

  private JobParamReader(JobParameters params) {
    this.params = Objects.requireNonNull(params);
  }

  public static JobParamReader of(JobParameters params) {
    return new JobParamReader(params);
  }

  public static JobParamReader fromStepExecution(StepExecution stepExecution) {
    return new JobParamReader(stepExecution.getJobExecution().getJobParameters());
  }

  public String getString(String key, String def) {
    var p = params.getString(key);
    return (p == null || p.isBlank()) ? def : p;
  }

  public boolean getBool(String key, boolean def) {
    var s = getString(key, null);
    if (s == null) return def;
    return Boolean.parseBoolean(s.trim());
  }

  public int getInt(String key, int def) {
    var s = getString(key, null);
    if (s == null) return def;
    try {
      return Integer.parseInt(s.trim());
    } catch (Exception ex) {
      return def;
    }
  }

  public List<String> getCsvListUpperDistinctSorted(String key) {
    var s = getString(key, "");
    if (s.isBlank()) return List.of();
    return Arrays.stream(s.split(","))
        .map(String::trim)
        .filter(v -> !v.isBlank())
        .map(v -> v.toUpperCase(Locale.ROOT))
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }
}
