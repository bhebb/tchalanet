import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/cashier_sell_catalog_models.dart';
import '../../data/models/cashier_ticket_models.dart';
import '../../data/services/cashier_sell_catalog_service.dart';
import '../../data/services/cashier_ticket_service.dart';

// ─── State ────────────────────────────────────────────────────────────────────

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
  });

  final List<CashierAvailableDrawView> draws;
  final List<CashierGameOptionResponse> games;
  final String? selectedDrawId;
  final String? selectedGameCode;
  final String? selectedBetType;
  final int? selectedBetOption;
  final String selection;
  final double stake;

  CashierAvailableDrawView? get selectedDraw =>
      draws.where((d) => d.drawId == selectedDrawId).firstOrNull;

  CashierGameOptionResponse? get selectedGame =>
      games.where((g) => g.gameCode == selectedGameCode).firstOrNull;

  bool get canPreview =>
      selectedDrawId != null &&
      selectedGameCode != null &&
      selection.trim().isNotEmpty &&
      stake > 0;

  SellFormData copyWith({
    List<CashierAvailableDrawView>? draws,
    List<CashierGameOptionResponse>? games,
    String? selectedDrawId,
    String? selectedGameCode,
    String? selectedBetType,
    Object? selectedBetOption = _sentinel,
    String? selection,
    double? stake,
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
  late final String _idempotencyKey;

  @override
  SellState build() {
    _idempotencyKey = _newKey();
    return const SellLoadingCatalog();
  }

  Future<void> loadCatalog({String? preselectedDrawId}) async {
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

      state = SellReady(SellFormData(
        draws: draws.where((d) => d.isOpen).toList(),
        games: games,
        selectedDrawId: drawId,
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

  Future<void> preview(String terminalId, String currency) async {
    final current = state;
    if (current is! SellReady || !current.form.canPreview) return;
    final form = current.form;

    state = SellPreviewing(form);
    try {
      final result = await ref.read(cashierTicketServiceProvider).preview(
            CashierTicketPreviewRequest(
              terminalId: terminalId,
              drawId: form.selectedDrawId!,
              drawChannelId: form.selectedDraw?.drawChannelId,
              currency: currency,
              lines: [
                CashierTicketLineRequest(
                  gameCode: form.selectedGameCode!,
                  betType: form.selectedBetType!,
                  selection: form.selection.trim(),
                  stake: form.stake,
                  betOption: form.selectedBetOption,
                ),
              ],
            ),
          );
      state = SellReady(form, previewResult: result);
    } catch (e) {
      state = SellReady(form, error: e.toString());
    }
  }

  Future<void> confirmSell(String terminalId, String currency) async {
    final current = state;
    if (current is! SellReady) return;
    final form = current.form;
    final preview = current.previewResult;
    if (preview == null || !preview.isAccepted) return;

    state = SellConfirming(form, preview);
    try {
      final response = await ref.read(cashierTicketServiceProvider).sell(
            CashierSellTicketRequest(
              terminalId: terminalId,
              drawId: form.selectedDrawId!,
              drawChannelId: form.selectedDraw?.drawChannelId,
              currency: currency,
              lines: [
                CashierTicketLineRequest(
                  gameCode: form.selectedGameCode!,
                  betType: form.selectedBetType!,
                  selection: form.selection.trim(),
                  stake: form.stake,
                  betOption: form.selectedBetOption,
                ),
              ],
            ),
            idempotencyKey: _idempotencyKey,
          );
      state = SellSuccess(response);
    } catch (e) {
      state = SellReady(form, previewResult: preview, error: e.toString());
    }
  }

  void reset() {
    _idempotencyKey = _newKey();
    state = const SellLoadingCatalog();
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
