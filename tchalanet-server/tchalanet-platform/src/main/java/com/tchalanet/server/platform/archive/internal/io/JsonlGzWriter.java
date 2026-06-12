package com.tchalanet.server.platform.archive.internal.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import tools.jackson.databind.ObjectMapper;

/**
 * Streaming writer for the {@code jsonl.gz} archive format.
 *
 * <p>One JSON object per line. Computes SHA-256 on the compressed bytes and
 * counts rows as rows are written.
 *
 * <p>Usage:
 * <pre>
 *   try (var writer = new JsonlGzWriter(outputStream, mapper)) {
 *       writer.write(row);
 *       ...
 *       long rows = writer.rowsWritten();
 *       String checksum = writer.checksumSha256();
 *       long bytes = writer.compressedBytes();
 *   }
 * </pre>
 */
public final class JsonlGzWriter implements AutoCloseable {

  private final ObjectMapper mapper;
  private final DigestOutputStream digestOut;
  private final GZIPOutputStream gzipOut;
  private final BufferedWriter writer;

  private long rowsWritten = 0;
  private boolean closed = false;
  private long compressedBytes = -1;

  public JsonlGzWriter(OutputStream out, ObjectMapper mapper) {
    this.mapper = mapper;
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      this.digestOut  = new DigestOutputStream(out, sha256);
      this.gzipOut    = new GZIPOutputStream(digestOut, 65536);
      this.writer     = new BufferedWriter(new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8), 65536);
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to initialize archive writer", e);
    }
  }

  public void write(Map<String, Object> row) {
    if (closed) throw new IllegalStateException("JsonlGzWriter is already closed");
    try {
      writer.write(mapper.writeValueAsString(row));
      writer.newLine();
      rowsWritten++;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write archive row", e);
    }
  }

  public long rowsWritten() {
    return rowsWritten;
  }

  public String checksumSha256() {
    if (!closed) throw new IllegalStateException("Must close before reading checksum");
    return HexFormat.of().formatHex(digestOut.getMessageDigest().digest());
  }

  public long compressedBytes() {
    return compressedBytes;
  }

  @Override
  public void close() throws IOException {
    if (closed) return;
    closed = true;
    writer.flush();
    gzipOut.finish();
    gzipOut.flush();
    writer.close();
    // compressedBytes is not easily available here without a counting wrapper;
    // the executor reads it from the storage backend after close.
  }
}
