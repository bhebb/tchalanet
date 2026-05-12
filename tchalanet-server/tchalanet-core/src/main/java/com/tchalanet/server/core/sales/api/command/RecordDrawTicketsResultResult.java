package com.tchalanet.server.core.sales.api.command;

public record RecordDrawTicketsResultResult(
    long processed,
    long won,
    long lost
) {}
