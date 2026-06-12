package com.tchalanet.server.platform.archive.internal.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Streaming reader for the {@code jsonl.gz} archive format.
 *
 * <p>Reads rows one at a time through GZIP decompression. Applies an optional
 * predicate to filter matching rows. Enforces a max-scan guard to prevent
 * runaway reads on large objects.
 */
public final class JsonlGzReader {

  private static final int DEFAULT_MAX_SCAN = 5_000_000;
  private static final TypeReference<Map<String, Object>> ROW_TYPE = new TypeReference<>() {};

  private final ObjectMapper mapper;
  private final int maxScan;

  public JsonlGzReader(ObjectMapper mapper) {
    this(mapper, DEFAULT_MAX_SCAN);
  }

  public JsonlGzReader(ObjectMapper mapper, int maxScan) {
    this.mapper  = mapper;
    this.maxScan = maxScan;
  }

  /**
   * Scan the compressed stream and return all rows matching {@code filter}.
   *
   * @param in     compressed input stream; caller is responsible for closing it
   * @param filter predicate to select rows; use {@code _ -> true} for all rows
   * @return matching rows (up to maxScan rows scanned)
   */
  public List<Map<String, Object>> readMatching(InputStream in, Predicate<Map<String, Object>> filter) {
    List<Map<String, Object>> results = new ArrayList<>();
    try (var gz     = new GZIPInputStream(in, 65536);
         var reader = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8), 65536)) {

      int scanned = 0;
      String line;
      while ((line = reader.readLine()) != null) {
        if (++scanned > maxScan) break;
        if (line.isBlank()) continue;
        Map<String, Object> row = mapper.readValue(line, ROW_TYPE);
        if (filter.test(row)) {
          results.add(row);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read archive object", e);
    }
    return results;
  }
}
