package com.tchalanet.server.common.job.context;

public interface JobContextBinder {

    void bind(JobContextBindingRequest request);

    void clear();
}
