import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/network/api_exception.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_sell_catalog_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/models/cashier_ticket_models.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_sell_catalog_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/data/services/cashier_ticket_service.dart';
import 'package:tchalanet_mobile/features/cashier/tickets/presentation/view_models/sell_controller.dart';

void main() {
  test(
    'prepare exposes shared validation fixture as translation keys',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(
          prepareError: _apiExceptionFromFixture(
            'problem-details/validation-failed.json',
          ),
        ),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);

      await controller.prepare();

      final state = container.read(sellControllerProvider);
      expect(state, isA<SellReady>());
      final ready = state as SellReady;
      expect(ready.errorKeys, [
        'common.errors.codes.validation.failed.message',
        'common.errors.categories.validation.message',
        'common.error.unknown',
      ]);
      expect(
        ready.errorKeys.join(' '),
        isNot(contains('Request could not be completed')),
      );
      expect(ready.form.allLines, hasLength(1));
    },
  );

  test(
    'confirm/sell preserves preview and exposes access denied translation keys',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(
          preview: _acceptedPreview(),
          confirmError: _apiExceptionFromFixture(
            'problem-details/access-denied.json',
          ),
        ),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);
      await controller.prepare();
      expect(container.read(sellControllerProvider), isA<SellReady>());
      expect(
        (container.read(sellControllerProvider) as SellReady)
            .previewResult
            ?.isAccepted,
        isTrue,
      );

      await controller.confirmSell();

      final state = container.read(sellControllerProvider);
      expect(state, isA<SellReady>());
      final ready = state as SellReady;
      expect(ready.previewResult?.preparationId, 'prep-1');
      expect(ready.errorKeys, [
        'common.errors.codes.access.denied.message',
        'common.errors.categories.access_denied.message',
        'common.error.unknown',
      ]);
    },
  );

  test(
    'confirm/sell reloads ticket details after an idempotent replay',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(
          preview: _acceptedPreview(),
          confirmResponse: const CashierSellTicketResponse(
            preparationId: 'prep-1',
            alreadyConfirmed: true,
            outcome: 'CONFIRMED',
            ticketId: 'ticket-1',
            ticketCode: '',
          ),
          details: const CashierTicketDetailsView(
            id: 'ticket-1',
            ticketCode: 'TCK-1',
            publicCode: 'PUB-1',
            status: 'PLACED',
            totalAmountCents: 1000,
            currency: 'HTG',
          ),
        ),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);
      await controller.prepare();

      await controller.confirmSell();

      final state = container.read(sellControllerProvider);
      expect(state, isA<SellSuccess>());
      final success = state as SellSuccess;
      expect(success.response.ticketId, 'ticket-1');
      expect(success.response.ticketCode, 'TCK-1');
      expect(success.response.publicCode, 'PUB-1');
    },
  );

  test(
    'quick sell confirms an accepted preparation in one seller action',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(preview: _acceptedPreview()),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);
      await controller.quickSell();

      expect(container.read(sellControllerProvider), isA<SellSuccess>());
    },
  );

  test(
    'regenerates a prepared promotion line and keeps the sale in preview',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(
          preview: _acceptedPreviewWithPromotion('12-34'),
          regeneratedPreview: _acceptedPreviewWithPromotion('56-78'),
        ),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);
      await controller.prepare();

      await controller.regeneratePromotionLine('promo-1');

      final state = container.read(sellControllerProvider);
      expect(state, isA<SellReady>());
      final ready = state as SellReady;
      expect(ready.previewResult?.preparationId, 'prep-1');
      expect(ready.previewResult?.promotionLines.single.selection, '56-78');
    },
  );

  test(
    'quick sell stops on generated promotion lines so the seller can regenerate',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(
          preview: _acceptedPreviewWithPromotion('12-34'),
        ),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);
      await controller.quickSell();

      final state = container.read(sellControllerProvider);
      expect(state, isA<SellReady>());
      final ready = state as SellReady;
      expect(ready.previewResult?.promotionLines.single.regenerable, isTrue);
    },
  );

  test('regenerate errors keep the original prepared promotion line', () async {
    final container = _containerWith(
      ticketService: _FakeTicketService(
        preview: _acceptedPreviewWithPromotion('12-34'),
        regenerateError: _apiExceptionFromFixture(
          'problem-details/validation-failed.json',
        ),
      ),
    );
    addTearDown(container.dispose);

    final controller = container.read(sellControllerProvider.notifier);
    await _loadReadySale(controller);
    await controller.prepare();
    await controller.regeneratePromotionLine('promo-1');

    final state = container.read(sellControllerProvider);
    expect(state, isA<SellReady>());
    final ready = state as SellReady;
    expect(ready.previewResult?.promotionLines.single.selection, '12-34');
    expect(
      ready.errorKeys.first,
      'common.errors.codes.validation.failed.message',
    );
  });

  test(
    'unknown confirm outcome keeps the prepared intent unresolved',
    () async {
      final container = _containerWith(
        ticketService: _FakeTicketService(
          preview: _acceptedPreview(),
          confirmError: ApiException(
            message: 'timeout',
            statusCode: 503,
            retryable: true,
          ),
        ),
      );
      addTearDown(container.dispose);

      final controller = container.read(sellControllerProvider.notifier);
      await _loadReadySale(controller);
      await controller.prepare();
      await controller.confirmSell();

      expect(container.read(sellControllerProvider), isA<SellConfirmUnknown>());
    },
  );
}

ProviderContainer _containerWith({
  required CashierTicketService ticketService,
}) {
  return ProviderContainer(
    overrides: [
      cashierSellCatalogServiceProvider.overrideWithValue(
        _FakeCatalogService(),
      ),
      cashierTicketServiceProvider.overrideWithValue(ticketService),
    ],
  );
}

Future<void> _loadReadySale(SellController controller) async {
  await controller.loadCatalog(currency: 'HTG');
  controller.selectGame(_game);
  controller.updateSelection('42');
  controller.updateStake(10);
}

CashierTicketPreviewResponse _acceptedPreview() {
  return const CashierTicketPreviewResponse(
    status: 'DRAFT',
    preparationId: 'prep-1',
    currency: 'HTG',
    totalAmount: 10,
  );
}

CashierTicketPreviewResponse _acceptedPreviewWithPromotion(String selection) {
  return CashierTicketPreviewResponse(
    status: 'DRAFT',
    preparationId: 'prep-1',
    currency: 'HTG',
    totalAmount: 10,
    promotionLines: [
      CashierPreparationPromotionLine(
        lineRef: 'promo-1',
        gameCode: 'HT_MARYAJ_GRATIS',
        betType: 'MARRIAGE_2D2D',
        selection: selection,
        selectionSource: 'PROMOTION_GENERATED',
        choiceMode: 'AUTO_GENERATE',
        promotionRuleKey: 'DEFAULT_MARYAJ_GRATIS',
        promotionEffectType: 'FREE_GAME_LINE',
        regenerable: true,
        regenerationsRemaining: 2,
      ),
    ],
  );
}

class _FakeCatalogService extends CashierSellCatalogService {
  _FakeCatalogService() : super(Dio());

  @override
  Future<List<CashierAvailableDrawView>> fetchAvailableDraws({
    int lookaheadHours = 24,
    int limit = 20,
  }) async => const [_draw];

  @override
  Future<List<CashierGameOptionResponse>> fetchAvailableGames() async => const [
    _game,
  ];
}

class _FakeTicketService extends CashierTicketService {
  _FakeTicketService({
    this.preview,
    this.prepareError,
    this.confirmError,
    this.regenerateError,
    this.confirmResponse,
    this.details,
    this.regeneratedPreview,
  }) : super(Dio());

  final CashierTicketPreviewResponse? preview;
  final ApiException? prepareError;
  final ApiException? confirmError;
  final ApiException? regenerateError;
  final CashierSellTicketResponse? confirmResponse;
  final CashierTicketDetailsView? details;
  final CashierTicketPreviewResponse? regeneratedPreview;

  @override
  Future<CashierTicketPreviewResponse> prepare(
    CashierTicketPreviewRequest request,
  ) async {
    if (prepareError != null) throw prepareError!;
    return preview ?? _acceptedPreview();
  }

  @override
  Future<CashierSellTicketResponse> confirm(
    String preparationId, {
    required String idempotencyKey,
  }) async {
    if (confirmError != null) throw confirmError!;
    return confirmResponse ??
        CashierSellTicketResponse(
          preparationId: preparationId,
          alreadyConfirmed: false,
          outcome: 'ACCEPTED',
          ticketId: 'ticket-1',
          ticketCode: 'TCK-1',
        );
  }

  @override
  Future<CashierTicketPreviewResponse> regeneratePromotionLine(
    String preparationId,
    String lineRef,
  ) async {
    if (regenerateError != null) throw regenerateError!;
    return regeneratedPreview ?? preview ?? _acceptedPreview();
  }

  @override
  Future<CashierTicketDetailsView> getDetails(String ticketId) async {
    return details ??
        CashierTicketDetailsView(
          id: ticketId,
          ticketCode: 'TCK-1',
          status: 'PLACED',
          totalAmountCents: 1000,
          currency: 'HTG',
        );
  }
}

ApiException _apiExceptionFromFixture(String relativePath) {
  final problem = _contractFixture(relativePath);
  return ApiException(
    message: 'Erreur serveur',
    statusCode: problem['status'] as int?,
    requestId: problem['requestId'] as String?,
    traceId: problem['traceId'] as String?,
    spanId: problem['spanId'] as String?,
    errorId: problem['errorId'] as String?,
    code: problem['code'] as String?,
    category: problem['category'] as String?,
    retryPolicy: problem['retryPolicy'] as String?,
    retryable: problem['retryable'] as bool? ?? false,
  );
}

Map<String, dynamic> _contractFixture(String relativePath) {
  final file = File(
    '../tchalanet-server/testing/contracts/error-contract/v1/$relativePath',
  );
  return jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
}

const _draw = CashierAvailableDrawView(
  drawId: 'draw-1',
  drawChannelId: 'channel-1',
  channelLabel: 'Haiti - NY',
  gameCodes: ['BOLET'],
  status: 'OPEN',
  providerDate: '2026-07-24',
  providerTime: '20:00:00',
  providerTimezone: 'America/New_York',
  localDate: '2026-07-24',
  localTime: '20:00:00',
  localTimezone: 'America/Port-au-Prince',
);

const _game = CashierGameOptionResponse(
  gameCode: 'BOLET',
  gameLabel: 'Bolet',
  betType: 'STRAIGHT',
  betTypeLabel: 'Direct',
  requiresOption: false,
  options: [],
  selectionDigits: 2,
  selectionSegments: 1,
);
