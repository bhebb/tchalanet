package com.tchalanet.server.app.job.aspect;

import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.exception.JobSkippedException;
import com.tchalanet.server.common.batch.service.BatchEventNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TchJobAspect {

    private final BatchEventNotificationService notifier;

    @Around("@annotation(job)")
    public Object around(ProceedingJoinPoint pjp, TchJob job) throws Throwable {
        var jobKey = job.value();

        try {
            notifier.started(jobKey);

            var result = pjp.proceed();

            notifier.succeeded(jobKey);
            return result;

        } catch (JobSkippedException e) {
            log.debug("batch.skipped jobKey={} code={} message={}",
                jobKey, e.code(), e.getMessage());

            notifier.skipped(jobKey, e.code(), e.getMessage());
            return null;

        } catch (Throwable e) {
            log.error("batch.failed jobKey={}", jobKey, e);

            notifier.failed(jobKey, e);
            throw e;
        }
    }
}
