package com.tchalanet.server.app.job.registry;

import com.tchalanet.server.common.job.registry.RegisteredJob;
import java.util.Objects;

/**
 * Runtime registration for a Spring Batch job.
 *
 * <p>This class lives in app because it contains the Spring Batch bean name.
 */
public record SpringTchRegisteredJob(
    RegisteredJob metadata,
    String springJobBeanName
) {

    public SpringTchRegisteredJob {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(springJobBeanName, "springJobBeanName");

        if (springJobBeanName.isBlank()) {
            throw new IllegalArgumentException("springJobBeanName cannot be blank");
        }
    }
}
