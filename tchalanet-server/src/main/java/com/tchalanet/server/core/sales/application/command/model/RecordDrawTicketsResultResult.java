package com.tchalanet.server.core.sales.application.command.model;

public record RecordDrawTicketsResultResult(
    long processed,
    long won,
    long lost
) {}
