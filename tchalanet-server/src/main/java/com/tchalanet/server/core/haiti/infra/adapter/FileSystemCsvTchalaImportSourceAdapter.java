package com.tchalanet.server.core.haiti.infra.adapter;

import com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand.ImportRow;
import com.tchalanet.server.core.haiti.application.port.out.TchalaImportSourcePort;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FileSystemCsvTchalaImportSourceAdapter implements TchalaImportSourcePort {

  @Override
  public List<ImportRow> readRows(String payloadRef) {
    if (payloadRef == null || payloadRef.isBlank()) {
      throw new IllegalArgumentException("payloadRef is blank");
    }

    Path path = Paths.get(payloadRef).normalize().toAbsolutePath();
    if (!Files.exists(path) || !Files.isRegularFile(path)) {
      throw new IllegalArgumentException("CSV file not found: " + path);
    }

    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      var rows = new ArrayList<ImportRow>();
      String line;
      int lineNo = 0;

      while ((line = br.readLine()) != null) {
        lineNo++;
        line = line.trim();
        if (line.isBlank() || line.startsWith("#")) continue;

        // Skip header if present
        if (lineNo == 1 && looksLikeHeader(line)) {
          continue;
        }

        List<String> cols = splitCsvLine(line, ';');
        // Accept 2 or 3 cols: dream;numbers;note?
        if (cols.size() < 2) {
          throw new IllegalArgumentException(
              "Invalid CSV (need at least 2 columns) at line " + lineNo);
        }

        String dream = cols.get(0).trim();
        String numbers = cols.get(1).trim();
        String note = (cols.size() >= 3) ? cols.get(2).trim() : "";

        if (dream.isBlank() || numbers.isBlank()) continue; // ignore empty
        rows.add(new ImportRow(dream, numbers, note));
      }

      return List.copyOf(rows);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CSV: " + e.getMessage(), e);
    }
  }

  private static boolean looksLikeHeader(String line) {
    String l = line.toLowerCase();
    return l.contains("dream")
        || l.contains("reve")
        || l.contains("numbers")
        || l.contains("numero");
  }

  /**
   * Split a CSV line by delimiter ';' supporting simple quotes: - "a;b";c -> [a;b, c] - "" inside
   * quotes is not fully supported; keep it simple.
   */
  private static List<String> splitCsvLine(String line, char delimiter) {
    var out = new ArrayList<String>();
    var sb = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);

      if (ch == '"') {
        inQuotes = !inQuotes;
        continue;
      }

      if (ch == delimiter && !inQuotes) {
        out.add(sb.toString());
        sb.setLength(0);
        continue;
      }

      sb.append(ch);
    }

    out.add(sb.toString());
    return out;
  }
}
