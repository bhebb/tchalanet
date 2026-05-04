package com.tchalanet.server.common.batch.aspect;

import com.tchalanet.server.common.batch.annotation.BatchScheduledJob;
import com.tchalanet.server.common.batch.exception.BatchSkippedException;
import com.tchalanet.server.common.batch.notification.BatchEventNotificationService;
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
public class BatchScheduledJobAspect {

    private final BatchEventNotificationService notifier;

    @Around("@annotation(job)")
    public Object around(ProceedingJoinPoint pjp, BatchScheduledJob job) throws Throwable {
        var jobKey = job.value();

        try {
            notifier.started(jobKey);

            var result = pjp.proceed();

            notifier.succeeded(jobKey);
            return result;

        } catch (BatchSkippedException e) {
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
