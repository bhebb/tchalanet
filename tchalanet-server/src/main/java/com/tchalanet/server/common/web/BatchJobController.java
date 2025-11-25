package com.tchalanet.server.common.web;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/jobs")
public class BatchJobController {

  private final ApplicationContext ctx;

  public BatchJobController(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @PostMapping("/launch")
  public ResponseEntity<?> launch(@RequestBody Map<String, String> body) {
    String jobName = body.get("jobName");
    if (jobName == null)
      return ResponseEntity.badRequest().body(Map.of("error", "jobName is required"));

    try {
      // Obtain job bean by name
      Object job = ctx.getBean(jobName);
      Object jobLauncher = ctx.getBean("jobLauncher");

      // Build JobParameters via reflection
      Class<?> jpbCls = Class.forName("org.springframework.batch.core.JobParametersBuilder");
      Object jpb = jpbCls.getConstructor().newInstance();
      Method addString = jpbCls.getMethod("addString", String.class, String.class);
      Method addDate = jpbCls.getMethod("addDate", String.class, java.util.Date.class);
      addString.invoke(jpb, "triggeredBy", body.getOrDefault("triggeredBy", "api"));
      addDate.invoke(jpb, "ts", java.util.Date.from(Instant.now()));
      Method toParams = jpbCls.getMethod("toJobParameters");
      Object jobParams = toParams.invoke(jpb);

      // Invoke jobLauncher.run(job, jobParams)
      Method run =
          jobLauncher
              .getClass()
              .getMethod(
                  "run",
                  Class.forName("org.springframework.batch.core.Job"),
                  Class.forName("org.springframework.batch.core.JobParameters"));
      Object exec = run.invoke(jobLauncher, job, jobParams);

      // read status/id reflectively
      Method getStatus = exec.getClass().getMethod("getStatus");
      Method getId = exec.getClass().getMethod("getId");
      Object status = getStatus.invoke(exec);
      Object id = getId.invoke(exec);

      return ResponseEntity.ok(Map.of("status", String.valueOf(status), "id", String.valueOf(id)));

    } catch (Exception e) {
      return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
  }
}
