import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/cashier_sell_catalog_models.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_sell_catalog_service.dart';
import '../../data/services/cashier_ticket_service.dart';

// ─── State ────────────────────────────────────────────────────────────────────

/// A committed bet line (added to the ticket, awaiting preview/confirm).
class SellLine {
  const SellLine({
    required this.gameCode,
    required this.gameLabel,
    required this.betType,
    required this.betTypeLabel,
    this.betOption,
    required this.selection,
    required this.stake,
  });

  final String gameCode;
  final String gameLabel;
  final String betType;
  final String betTypeLabel;
  final int? betOption;
  final String selection;
  final double stake;

  CashierTicketLineRequest toRequest() => CashierTicketLineRequest(
        gameCode: gameCode,
        betType: betType,
        selection: selection,
        stake: stake,
        betOption: betOption,
      );

  /// Display label for the committed line chip, e.g. "Bolet  42 – 10.00"
  /// Lot variants (1er lot…) are omitted — the payout engine handles them.
  String get displayLabel =>
      '$gameLabel  $selection  –  ${stake.toStringAsFixed(2)}';
}

class SellFormData {
  const SellFormData({
    this.draws = const [],
    this.games = const [],
    this.selectedDrawId,
    this.selectedGameCode,
    this.selectedBetType,
    this.selectedBetOption,
    this.selection = '',
    this.stake = 0.0,
    this.committedLines = const [],
    this.currency = 'HTG', // default safe fallback; overridden from home response
  });

  final List<CashierAvailableDrawView> draws;
  final List<CashierGameOptionResponse> games;
  final String? selectedDrawId;
  final String? selectedGameCode;
  final String? selectedBetType;
  final int? selectedBetOption;
  final String selection;
  final double stake;
  final String currency;

  /// Lines already added to the ticket (will be sent together in preview/confirm).
  final List<SellLine> committedLines;

  CashierAvailableDrawView? get selectedDraw =>
      draws.where((d) => d.drawId == selectedDrawId).firstOrNull;

  CashierGameOptionResponse? get selectedGame => games
      .where((g) =>
          g.gameCode == selectedGameCode && g.betType == selectedBetType)
      .firstOrNull;

  /// True when the current entry (selection + stake) forms a valid line.
  bool get canAddLine =>
      selectedGameCode != null &&
      selection.trim().isNotEmpty &&
      stake > 0;

  /// True when there is at least one line ready to preview (committed or current).
  bool get canPreview =>
      selectedDrawId != null &&
      (committedLines.isNotEmpty || canAddLine);

  /// All lines to send: committed ones plus the current entry if valid.
  List<SellLine> get allLines {
    if (!canAddLine) return committedLines;
    final game = selectedGame;
    return [
      ...committedLines,
      SellLine(
        gameCode: selectedGameCode!,
        gameLabel: game?.gameLabel ?? selectedGameCode!,
        betType: selectedBetType!,
        betTypeLabel: game?.betTypeLabel ?? '',
        betOption: selectedBetOption,
        selection: selection.trim(),
        stake: stake,
      ),
    ];
  }

  SellFormData copyWith({
    List<CashierAvailableDrawView>? draws,
    List<CashierGameOptionResponse>? games,
    String? selectedDrawId,
    String? selectedGameCode,
    String? selectedBetType,
    Object? selectedBetOption = _sentinel,
    String? selection,
    double? stake,
    String? currency,
    List<SellLine>? committedLines,
  }) =>
      SellFormData(
        draws: draws ?? this.draws,
        games: games ?? this.games,
        selectedDrawId: selectedDrawId ?? this.selectedDrawId,
        selectedGameCode: selectedGameCode ?? this.selectedGameCode,
        selectedBetType: selectedBetType ?? this.selectedBetType,
        selectedBetOption: selectedBetOption == _sentinel
            ? this.selectedBetOption
            : selectedBetOption as int?,
        selection: selection ?? this.selection,
        stake: stake ?? this.stake,
        currency: currency ?? this.currency,
        committedLines: committedLines ?? this.committedLines,
      );
}

const _sentinel = Object();

sealed class SellState {
  const SellState();
}

final class SellLoadingCatalog extends SellState {
  const SellLoadingCatalog();
}

final class SellReady extends SellState {
  const SellReady(this.form, {this.previewResult, this.error});
  final SellFormData form;
  final CashierTicketPreviewResponse? previewResult;
  final String? error;
}

final class SellPreviewing extends SellState {
  const SellPreviewing(this.form);
  final SellFormData form;
}

final class SellConfirming extends SellState {
  const SellConfirming(this.form, this.preview);
  final SellFormData form;
  final CashierTicketPreviewResponse preview;
}

final class SellSuccess extends SellState {
  const SellSuccess(this.response);
  final CashierSellTicketResponse response;
}

final class SellCatalogError extends SellState {
  const SellCatalogError(this.message);
  final String message;
}

// ─── Controller ───────────────────────────────────────────────────────────────

class SellController extends Notifier<SellState> {
  // Not `final`: the provider is kept alive across navigations, so each new sell
  // flow (page entry / reset) must mint a fresh idempotency key — otherwise a
  // second ticket would reuse the first ticket's key and the backend would
  // dedupe it. Regenerated at the start of loadCatalog().
  late String _idempotencyKey;

  @override
  SellState build() {
    _idempotencyKey = _newKey();
    return const SellLoadingCatalog();
  }

  Future<void> loadCatalog({
    String? preselectedDrawId,
    String? currency,
  }) async {
    // Fresh sell flow → fresh idempotency key (one ticket = one key).
    _idempotencyKey = _newKey();
    state = const SellLoadingCatalog();
    try {
      final catalog = ref.read(cashierSellCatalogServiceProvider);
      final results = await Future.wait([
        catalog.fetchAvailableDraws(),
        catalog.fetchAvailableGames(),
      ]);
      final draws = results[0] as List<CashierAvailableDrawView>;
      final games = results[1] as List<CashierGameOptionResponse>;

      final drawId = preselectedDrawId ??
          draws.where((d) => d.isOpen).firstOrNull?.drawId;

      // Currency from home response; fallback to HTG only if not provided.
      final resolvedCurrency = currency ?? 'HTG';

      state = SellReady(SellFormData(
        draws: draws.where((d) => d.isOpen).toList(),
        games: games,
        selectedDrawId: drawId,
        currency: resolvedCurrency,
      ));
    } catch (e) {
      state = SellCatalogError(e.toString());
    }
  }

  void selectDraw(String drawId) {
    final current = state;
    if (current is! SellReady) return;
    state = SellReady(
      current.form.copyWith(selectedDrawId: drawId),
      previewResult: null,
    );
  }

  void selectGame(CashierGameOptionResponse game) {
    final current = state;
    if (current is! SellReady) return;
    state = SellReady(
      current.form.copyWith(
        selectedGameCode: game.gameCode,
        selectedBetType: game.betType,
        selectedBetOption: null,
        selection: '',
      ),
      previewResult: null,
    );
  }

  void selectBetOption(int option) {
    final current = state;
    if (current is! SellReady) return;
    state = SellReady(
      current.form.copyWith(selectedBetOption: option),
      previewResult: null,
    );
  }

  void updateSelection(String value) {
    final current = state;
    if (current is! SellReady) return;
    state = SellReady(
      current.form.copyWith(selection: value),
      previewResult: null,
    );
  }

  void updateStake(double value) {
    final current = state;
    if (current is! SellReady) return;
    state = SellReady(
      current.form.copyWith(stake: value),
      previewResult: null,
    );
  }

  /// Commits the current entry (selection + stake) to the lines list and
  /// resets the entry fields so the user can add another line.
  /// The draw and game selection are kept so the user can quickly add
  /// a second bet on the same game.
  void addLine() {
    final current = state;
    if (current is! SellReady || !current.form.canAddLine) return;
    final form = current.form;
    final game = form.selectedGame;
    final newLine = SellLine(
      gameCode: form.selectedGameCode!,
      gameLabel: game?.gameLabel ?? form.selectedGameCode!,
      betType: form.selectedBetType!,
      betTypeLabel: game?.betTypeLabel ?? '',
      betOption: form.selectedBetOption,
      selection: form.selection.trim(),
      stake: form.stake,
    );
    state = SellReady(
      form.copyWith(
        committedLines: [...form.committedLines, newLine],
        // Reset current entry but keep draw + game selected.
        selection: '',
        stake: 0.0,
      ),
      previewResult: null, // Preview is invalidated when lines change.
    );
  }

  /// Removes a committed line by index.
  void removeLine(int index) {
    final current = state;
    if (current is! SellReady) return;
    final lines = [...current.form.committedLines];
    if (index < 0 || index >= lines.length) return;
    lines.removeAt(index);
    state = SellReady(
      current.form.copyWith(committedLines: lines),
      previewResult: null,
    );
  }

  Future<void> preview(String terminalId) async {
    final current = state;
    if (current is! SellReady || !current.form.canPreview) return;
    final form = current.form;
    final lines = form.allLines;
    if (lines.isEmpty) return;

    state = SellPreviewing(form);
    try {
      final result = await ref.read(cashierTicketServiceProvider).preview(
            CashierTicketPreviewRequest(
              terminalId: terminalId,
              drawId: form.selectedDrawId!,
              drawChannelId: form.selectedDraw?.drawChannelId,
              currency: form.currency,
              lines: lines.map((l) => l.toRequest()).toList(),
            ),
          );
      state = SellReady(form, previewResult: result);
    } catch (e) {
      state = SellReady(form, error: e.toString());
    }
  }

  Future<void> confirmSell(String terminalId) async {
    final current = state;
    if (current is! SellReady) return;
    final form = current.form;
    final preview = current.previewResult;
    if (preview == null || !preview.isAccepted) return;
    final lines = form.allLines;
    if (lines.isEmpty) return;

    state = SellConfirming(form, preview);
    try {
      final response = await ref.read(cashierTicketServiceProvider).sell(
            CashierSellTicketRequest(
              terminalId: terminalId,
              drawId: form.selectedDrawId!,
              drawChannelId: form.selectedDraw?.drawChannelId,
              currency: form.currency,
              lines: lines.map((l) => l.toRequest()).toList(),
            ),
            idempotencyKey: _idempotencyKey,
          );
      state = SellSuccess(response);
    } catch (e) {
      state = SellReady(form, previewResult: preview, error: e.toString());
    }
  }

  void reset() {
    // loadCatalog() mints a fresh idempotency key and resets the form state.
    loadCatalog();
  }

  static String _newKey() {
    final rand = Random.secure();
    final bytes = List.generate(16, (_) => rand.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final h = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
  }
}

final sellControllerProvider =
    NotifierProvider<SellController, SellState>(SellController.new);
