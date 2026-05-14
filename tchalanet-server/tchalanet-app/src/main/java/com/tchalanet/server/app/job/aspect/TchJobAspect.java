package com.tchalanet.server.app.job.aspect;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.job.annotation.TchJob;
import com.tchalanet.server.common.job.exception.JobSkippedException;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleEvent;
import com.tchalanet.server.common.job.lifecycle.JobLifecycleStatus;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import java.time.Clock;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TchJobAspect {

    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final TchContextResolver contextResolver;

    @Around("@annotation(job)")
    public Object around(ProceedingJoinPoint pjp, TchJob job) throws Throwable {
        var jobKey = job.value();

        publish(jobKey, JobLifecycleStatus.STARTED, null, null, Map.of());

        try {
            var result = pjp.proceed();

            publish(jobKey, JobLifecycleStatus.SUCCEEDED, null, null, Map.of());
            return result;

        } catch (JobSkippedException e) {
            publish(jobKey, JobLifecycleStatus.SKIPPED, e.code(), e.getMessage(), Map.of());
            return null;

        } catch (Throwable e) {
            publish(
                jobKey,
                JobLifecycleStatus.FAILED,
                e.getClass().getSimpleName(),
                e.getMessage(),
                Map.of("exception", e.getClass().getName())
            );

            throw e;
        }
    }

    private void publish(
        String jobKey,
        JobLifecycleStatus status,
        String code,
        String message,
        Map<String, Object> details
    ) {
        try {
            var ctx = contextResolver.currentOrNull();

            events.publishEvent(new JobLifecycleEvent(
                EventId.of(idGenerator.newUuid()),
                clock.instant(),
                ctx == null ? null : ctx.effectiveTenantIdOrNull(),
                ctx == null ? null : ctx.requestId(),
                jobKey,
                status,
                code,
                message,
                details
            ));
        } catch (Exception e) {
            log.warn(
                "job.lifecycle.publish.failed jobKey={} status={} code={}",
                jobKey,
                status,
                code,
                e
            );
        }
    }
}
