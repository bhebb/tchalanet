import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'client_diagnostics_models.dart';
import 'client_diagnostics_reporter.dart';

void installClientDiagnosticsErrorHooks(ProviderContainer container) {
  final previousFlutterHandler = FlutterError.onError;
  FlutterError.onError = (FlutterErrorDetails details) {
    _safeRecord(
      container,
      ClientDiagnosticEvent(
        category: ClientDiagnosticCategory.flutter,
        occurredAtClient: DateTime.now().toUtc(),
        severity: ClientDiagnosticSeverity.error,
        operation: 'flutter.framework',
        message: details.exceptionAsString(),
        exceptionType: details.exception.runtimeType.toString(),
        stackFrames: _stackFrames(details.stack),
      ),
    );

    if (previousFlutterHandler != null) {
      previousFlutterHandler(details);
    } else {
      FlutterError.presentError(details);
    }
  };

  final previousPlatformHandler = ui.PlatformDispatcher.instance.onError;
  ui.PlatformDispatcher.instance.onError = (Object error, StackTrace stack) {
    _safeRecord(
      container,
      ClientDiagnosticEvent(
        category: ClientDiagnosticCategory.async,
        occurredAtClient: DateTime.now().toUtc(),
        severity: ClientDiagnosticSeverity.error,
        operation: 'dart.uncaught_async',
        message: error.toString(),
        exceptionType: error.runtimeType.toString(),
        stackFrames: _stackFrames(stack),
      ),
    );

    return previousPlatformHandler?.call(error, stack) ?? false;
  };
}

void _safeRecord(ProviderContainer container, ClientDiagnosticEvent event) {
  try {
    container.read(clientDiagnosticsReporterProvider).record(event);
  } catch (_) {
    // Diagnostics must never create another crash path.
  }
}

List<ClientDiagnosticStackFrame> _stackFrames(StackTrace? stack) {
  if (stack == null) return const [];
  return stack
      .toString()
      .split('\n')
      .map((line) => line.trim())
      .where((line) => line.isNotEmpty)
      .take(24)
      .map((line) => ClientDiagnosticStackFrame(symbol: line))
      .toList(growable: false);
}
