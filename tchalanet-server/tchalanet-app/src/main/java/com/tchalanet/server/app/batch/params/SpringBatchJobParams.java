package com.tchalanet.server.app.batch.params;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class SpringBatchJobParams {

    private SpringBatchJobParams() {
    }

    public static Map<String, String> toStringMap(JobParameters params) {
        Map<String, String> out = new HashMap<>();

        for (Iterator<JobParameter<?>> it = params.parameters().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Object value = entry.value();
            if (value != null) {
                out.put(entry.name(), value.toString());
            }
        }

        return out;
    }
}
