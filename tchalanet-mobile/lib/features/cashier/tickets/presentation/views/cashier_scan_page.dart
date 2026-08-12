import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../core/network/api_exception.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_ticket_service.dart';
import '../ticket_verification_copy.dart';

// ─── State ────────────────────────────────────────────────────────────────────

sealed class VerifyState {
  const VerifyState();
}

final class VerifyIdle extends VerifyState {
  const VerifyIdle();
}

final class VerifyInProgress extends VerifyState {
  const VerifyInProgress();
}

final class VerifyResult extends VerifyState {
  const VerifyResult(this.response, this.scannedValue);
  final CashierTicketVerificationResponse response;
  final String scannedValue;
}

final class VerifyError extends VerifyState {
  const VerifyError(this.errorKeys);
  final List<String> errorKeys;
}

class VerifyController extends Notifier<VerifyState> {
  @override
  VerifyState build() => const VerifyIdle();

  Future<CashierTicketVerificationResponse?> verify(String scannedValue) async {
    if (scannedValue.trim().isEmpty) return null;
    state = const VerifyInProgress();
    try {
      final result = await ref
          .read(cashierTicketServiceProvider)
          .verify(
            CashierVerifyTicketRequest(scannedValue: scannedValue.trim()),
          );
      state = VerifyResult(result, scannedValue.trim());
      return result;
    } catch (e) {
      state = VerifyError(userErrorTranslationKeys(e));
      return null;
    }
  }

  void reset() => state = const VerifyIdle();
}

final verifyControllerProvider =
    NotifierProvider<VerifyController, VerifyState>(VerifyController.new);

// ─── Page ─────────────────────────────────────────────────────────────────────

class CashierScanPage extends ConsumerStatefulWidget {
  const CashierScanPage({super.key, this.initialCode});

  final String? initialCode;

  @override
  ConsumerState<CashierScanPage> createState() => _CashierScanPageState();
}

class _CashierScanPageState extends ConsumerState<CashierScanPage> {
  final _controller = TextEditingController();

  @override
  void initState() {
    super.initState();
    _applyInitialCode(widget.initialCode);
  }

  @override
  void didUpdateWidget(CashierScanPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.initialCode != widget.initialCode) {
      _applyInitialCode(widget.initialCode);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(verifyControllerProvider);
    final translations = ref.watch(i18nBundleProvider);
    final isLoading = state is VerifyInProgress;
    final canVerify = _controller.text.trim().isNotEmpty && !isLoading;

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('pos.ticket.verify.title')),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          tooltip: translations.translate('pos.ticket.verify.back_home'),
          onPressed: () => context.go('/pos'),
        ),
        actions: [
          if (state is VerifyResult || state is VerifyError)
            TextButton(
              onPressed: () {
                _controller.clear();
                ref.read(verifyControllerProvider.notifier).reset();
              },
              child: Text(translations.translate('pos.ticket.verify.new')),
            ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(TchSpacing.s24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Manual input
                    TextField(
                      controller: _controller,
                      enabled: !isLoading,
                      textCapitalization: TextCapitalization.characters,
                      inputFormatters: [
                        FilteringTextInputFormatter.allow(
                          RegExp(r'[A-Za-z0-9\-:/?&=._%+#]'),
                        ),
                        UpperCaseTextFormatter(),
                      ],
                      onSubmitted: (_) => _verify(),
                      decoration: InputDecoration(
                        labelText: translations.translate(
                          'pos.ticket.verify.code_label',
                        ),
                        hintText: translations.translate(
                          'pos.ticket.verify.code_hint',
                        ),
                        prefixIcon: const Icon(
                          Icons.confirmation_number_outlined,
                        ),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(TchRadius.md),
                        ),
                        suffixIcon: _controller.text.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear_rounded),
                                onPressed: () {
                                  _controller.clear();
                                  setState(() {});
                                },
                              )
                            : null,
                      ),
                      onChanged: (_) => setState(() {}),
                    ),

                    const SizedBox(height: TchSpacing.s24),

                    // Result
                    if (state is VerifyResult)
                      _VerifyResultCard(
                        result: state.response,
                        translations: translations,
                      )
                    else if (state is VerifyError)
                      _ErrorCard(
                        message: _translateError(translations, state.errorKeys),
                      ),
                  ],
                ),
              ),
            ),

            // CTA
            Padding(
              padding: const EdgeInsets.fromLTRB(
                TchSpacing.s24,
                TchSpacing.s8,
                TchSpacing.s24,
                TchSpacing.s24,
              ),
              child: Column(
                children: [
                  // View detail action — only enabled when ticketId is available
                  if (state is VerifyResult) ...[
                    SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: OutlinedButton.icon(
                        onPressed: state.response.ticketId != null
                            ? () => context.push(
                                '/pos/tickets/${state.response.ticketId}',
                                extra: state.response,
                              )
                            : null,
                        icon: const Icon(Icons.receipt_long_rounded),
                        label: Text(
                          translations.translate('pos.ticket.verify.details'),
                        ),
                        style: OutlinedButton.styleFrom(
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(TchRadius.md),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s8),
                  ],

                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: OutlinedButton.icon(
                      onPressed: isLoading ? null : _scanWithCamera,
                      icon: const Icon(Icons.qr_code_scanner_rounded),
                      label: Text(
                        translations.translate('pos.ticket.verify.scan_qr'),
                      ),
                      style: OutlinedButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(TchRadius.md),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: TchSpacing.s8),

                  // Verify button
                  SizedBox(
                    width: double.infinity,
                    height: state is VerifyResult ? 48 : 56,
                    child: FilledButton.icon(
                      onPressed: canVerify ? _verify : null,
                      icon: isLoading
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: TchColors.onPrimary,
                              ),
                            )
                          : const Icon(Icons.search_rounded),
                      label: Text(
                        isLoading
                            ? translations.translate(
                                'pos.ticket.verify.checking',
                              )
                            : translations.translate('pos.ticket.verify.check'),
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.5,
                        ),
                      ),
                      style: FilledButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(TchRadius.md),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _verify() async {
    final value = _controller.text.trim();
    if (value.isEmpty) return;
    final result = await ref
        .read(verifyControllerProvider.notifier)
        .verify(value);
    if (!mounted) return;
    final ticketId = result?.ticketId?.trim();
    if (ticketId == null || ticketId.isEmpty) return;
    unawaited(context.push('/pos/tickets/$ticketId', extra: result));
  }

  Future<void> _scanWithCamera() async {
    final scannedValue = await Navigator.of(context).push<String>(
      MaterialPageRoute(
        builder: (_) => const _TicketQrScannerPage(),
        fullscreenDialog: true,
      ),
    );
    final value = scannedValue?.trim();
    if (value == null || value.isEmpty || !mounted) return;
    setState(() => _controller.text = value.toUpperCase());
    unawaited(_verify());
  }

  void _applyInitialCode(String? value) {
    final code = value?.trim();
    if (code == null || code.isEmpty) return;
    _controller.text = code.toUpperCase();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      unawaited(_verify());
    });
  }
}

// ─── Verification result card ─────────────────────────────────────────────────

class _VerifyResultCard extends StatelessWidget {
  const _VerifyResultCard({required this.result, required this.translations});

  final CashierTicketVerificationResponse result;
  final I18nBundle translations;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    final (bgColor, fgColor, borderColor, icon) = switch (result.severity) {
      'SUCCESS' => (
        TchColors.successContainer,
        TchColors.success,
        TchColors.successContainer,
        Icons.check_circle_outline_rounded,
      ),
      'WARNING' => (
        TchColors.warningContainer,
        TchColors.warning,
        TchColors.warning,
        Icons.warning_amber_rounded,
      ),
      'ERROR' => (
        scheme.errorContainer,
        scheme.onErrorContainer,
        scheme.error.withValues(alpha: 0.3),
        Icons.cancel_outlined,
      ),
      _ => (
        scheme.surfaceContainerLow,
        scheme.onSurface,
        scheme.outlineVariant,
        Icons.info_outline_rounded,
      ),
    };

    final title = translateTicketVerificationCopy(
      translations,
      result.titleKey,
      fallback: 'pos.ticket.verify.unknown.title',
      params: result.params,
    );
    final message = translateTicketVerificationCopy(
      translations,
      result.messageKey,
      fallback: 'pos.ticket.verify.unknown.message',
      params: result.params,
    );

    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: fgColor, size: 22),
              const SizedBox(width: TchSpacing.s8),
              Expanded(
                child: Text(
                  title,
                  style: textTheme.titleSmall?.copyWith(
                    color: fgColor,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          if (message.isNotEmpty) ...[
            const SizedBox(height: TchSpacing.s12),
            Text(message, style: textTheme.bodySmall?.copyWith(color: fgColor)),
          ],
        ],
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: scheme.errorContainer,
        borderRadius: BorderRadius.circular(TchRadius.md),
      ),
      child: Row(
        children: [
          Icon(
            Icons.error_outline_rounded,
            size: 18,
            color: scheme.onErrorContainer,
          ),
          const SizedBox(width: TchSpacing.s8),
          Expanded(
            child: Text(
              message,
              style: Theme.of(
                context,
              ).textTheme.bodySmall?.copyWith(color: scheme.onErrorContainer),
            ),
          ),
        ],
      ),
    );
  }
}

class _TicketQrScannerPage extends ConsumerStatefulWidget {
  const _TicketQrScannerPage();

  @override
  ConsumerState<_TicketQrScannerPage> createState() =>
      _TicketQrScannerPageState();
}

class _TicketQrScannerPageState extends ConsumerState<_TicketQrScannerPage> {
  late final MobileScannerController _scannerController;
  bool _resolved = false;

  @override
  void initState() {
    super.initState();
    _scannerController = MobileScannerController(
      detectionSpeed: DetectionSpeed.noDuplicates,
      formats: const [BarcodeFormat.qrCode],
    );
  }

  @override
  void dispose() {
    _scannerController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final translations = ref.watch(i18nBundleProvider);
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('pos.ticket.verify.scan_title')),
        actions: [
          IconButton(
            tooltip: translations.translate('pos.ticket.verify.scan_torch'),
            onPressed: () => unawaited(_scannerController.toggleTorch()),
            icon: const Icon(Icons.flashlight_on_rounded),
          ),
        ],
      ),
      body: Stack(
        children: [
          MobileScanner(
            controller: _scannerController,
            onDetect: _handleDetection,
          ),
          Positioned.fill(
            child: IgnorePointer(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  border: Border.all(
                    color: scheme.primary.withValues(alpha: 0.84),
                    width: 4,
                  ),
                ),
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(TchSpacing.s24),
              color: scheme.surface.withValues(alpha: 0.92),
              child: Text(
                translations.translate('pos.ticket.verify.scan_prompt'),
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: scheme.onSurface,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _handleDetection(BarcodeCapture capture) {
    if (_resolved) return;
    String? value;
    for (final barcode in capture.barcodes) {
      final candidate = barcode.rawValue?.trim();
      if (candidate != null && candidate.isNotEmpty) {
        value = candidate;
        break;
      }
    }
    if (value == null) return;
    _resolved = true;
    Navigator.of(context).pop(value);
  }
}

String _translateError(I18nBundle translations, List<String> keys) {
  for (final key in keys) {
    final translated = translations.translate(key);
    if (translated != key) return translated;
  }
  return translations.translate('common.error.unknown');
}

// ─── Formatter ────────────────────────────────────────────────────────────────

class UpperCaseTextFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue old,
    TextEditingValue updated,
  ) {
    return updated.copyWith(text: updated.text.toUpperCase());
  }
}
