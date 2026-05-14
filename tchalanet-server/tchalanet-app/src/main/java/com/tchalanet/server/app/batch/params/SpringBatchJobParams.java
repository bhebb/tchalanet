package com.tchalanet.server.app.batch.params;

import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;

public final class SpringBatchJobParams {

    private SpringBatchJobParams() {}

    public static Map<String, String> toStringMap(JobParameters params) {
        Map<String, String> out = new HashMap<>();

        for (JobParameter<?> parameter : params.parameters()) {
            Object value = parameter.value();

            if (value != null) {
                out.put(parameter.name(), value.toString());
            }
        }

        return out;
    }
}
