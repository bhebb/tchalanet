package com.tchalanet.server.core.haiti.application.command.model;

/** Report/result for an import operation of Tchala entries. */
public record ImportTchalaReport(
    int totalRows,
    int parsedRows,
    int createdPending,
    int createdCanonical,
    int mergedIntoCanonical,
    int conflicts,
    int duplicatesInFile) {}
