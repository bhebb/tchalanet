package com.tchalanet.server.platform.clientdiagnostics.internal.service;

import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticStackFrame;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class ClientDiagnosticRedactor {

  private static final Pattern EMAIL =
      Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
  private static final Pattern LONG_DIGIT_RUN = Pattern.compile("\\d{8,}");
  private static final Pattern SECRET_WORD =
      Pattern.compile(
          "(password|passwd|token|authorization|bearer|secret|api[_-]?key|pin)",
          Pattern.CASE_INSENSITIVE);

  boolean isSafe(ClientDiagnosticEventRequest event) {
    return isSafeText(event.message())
        && isSafeText(event.errorCode())
        && isSafeText(event.exceptionType())
        && isSafeText(event.requestId())
        && isSafeText(event.correlationId())
        && isSafeText(event.endpointKey())
        && (event.stackFrames() == null
            || event.stackFrames().stream().allMatch(this::isSafeFrame));
  }

  private boolean isSafeFrame(ClientDiagnosticStackFrame frame) {
    return isSafeText(frame.symbol()) && isSafeText(frame.file());
  }

  private boolean isSafeText(String value) {
    if (value == null || value.isBlank()) return true;
    var normalized = value.toLowerCase(Locale.ROOT);
    return !EMAIL.matcher(value).find()
        && !LONG_DIGIT_RUN.matcher(value).find()
        && !SECRET_WORD.matcher(normalized).find();
  }
}
