import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../core/network/connectivity_repository.dart';
import '../../../../../design_system/components/components.dart';
import '../../../../../design_system/layout/screen_size.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../../../../notifications/presentation/view_models/notification_summary_controller.dart';
import '../../../tickets/data/models/cashier_sell_catalog_models.dart';
import '../../../tickets/data/models/cashier_ticket_models.dart';
import '../../../tickets/presentation/cashier_draw_label.dart';
import '../../../tickets/presentation/print_ticket_action.dart';
import '../../data/models/cashier_home_models.dart';
import '../../data/services/terminal_stats_service.dart';
import '../view_models/cashier_home_providers.dart';
import 'seller_terminal_nav_bar.dart';

class CashierHomePage extends ConsumerStatefulWidget {
  const CashierHomePage({super.key});

  @override
  ConsumerState<CashierHomePage> createState() => _CashierHomePageState();
}

class _CashierHomePageState extends ConsumerState<CashierHomePage> {
  bool _pinRedirectScheduled = false;

  @override
  Widget build(BuildContext context) {
    final homeAsync = ref.watch(cashierHomeProvider);
    final translations = ref.watch(i18nBundleProvider);

    return homeAsync.when(
      loading: () => const _LoadingScaffold(),
      error: (_, _) => _ErrorScaffold(translations: translations, ref: ref),
      data: (home) {
        if (home.requiredStep != null) {
          if (home.mustChangePin && !_pinRedirectScheduled) {
            _pinRedirectScheduled = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (mounted) context.go('/change-pin');
            });
          }
          return _BlockedStepScaffold(home: home);
        }
        return _SellerTerminalHome(home: home);
      },
    );
  }
}

class _LoadingScaffold extends StatelessWidget {
  const _LoadingScaffold();

  @override
  Widget build(BuildContext context) => const Scaffold(
    body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TchBrandMark(size: 72),
          SizedBox(height: TchSpacing.s20),
          CircularProgressIndicator(),
        ],
      ),
    ),
  );
}

class _ErrorScaffold extends StatelessWidget {
  const _ErrorScaffold({required this.translations, required this.ref});

  final I18nBundle translations;
  final WidgetRef ref;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: const _PosAppBar(terminalLabel: null),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(TchSpacing.s24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
              const SizedBox(height: TchSpacing.s16),
              Text(
                translations.translate('common.error.network'),
                style: Theme.of(context).textTheme.titleMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: TchSpacing.s20),
              TonalActionButton(
                label: translations.translate('common.retry'),
                onPressed: () => ref.invalidate(cashierHomeProvider),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BlockedStepScaffold extends ConsumerWidget {
  const _BlockedStepScaffold({required this.home});

  final CashierHomeResponse home;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final translations = ref.watch(i18nBundleProvider);
    final step = home.requiredStep!;
    final isPinChange = step.type == 'MUST_CHANGE_PIN';
    final title = isPinChange
        ? translations.translate('auth.change_pin.title')
        : step.title;
    final message = isPinChange
        ? translations.translate('auth.change_pin.description')
        : step.message;

    return Scaffold(
      appBar: _PosAppBar(
        terminalLabel: home.operationalContext?.sellerTerminalLabel,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(TchSpacing.s24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              Icon(Icons.lock_outline_rounded, size: 64, color: scheme.primary),
              const SizedBox(height: TchSpacing.s24),
              Text(
                title,
                style: textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: TchSpacing.s12),
              Text(
                message,
                style: textTheme.bodyMedium?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(),
              PrimaryActionButton(
                label: translations.translate('auth.change_pin.title'),
                icon: Icons.password_rounded,
                onPressed: () async {
                  await context.push('/change-pin');
                  ref.invalidate(cashierHomeProvider);
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SellerTerminalHome extends ConsumerStatefulWidget {
  const _SellerTerminalHome({required this.home});

  final CashierHomeResponse home;

  @override
  ConsumerState<_SellerTerminalHome> createState() =>
      _SellerTerminalHomeState();
}

class _SellerTerminalHomeState extends ConsumerState<_SellerTerminalHome> {
  bool _providerFilterExpanded = false;
  String? _selectedProvider;

  Future<void> _refresh() async {
    ref.invalidate(cashierHomeProvider);
    ref.invalidate(cashierReadinessProvider);
    ref.invalidate(terminalDailyStatsProvider);
    ref.invalidate(availableDrawsProvider);
    ref.invalidate(latestTicketProvider);
  }

  /// Tapping the card anywhere outside the sell button opens that draw's report.
  void _openDrawReport(CashierAvailableDrawView draw) {
    if (draw.drawId.isEmpty) return;
    final translations = ref.read(i18nBundleProvider);
    context.push(
      '/pos/reports/draw/${draw.drawId}',
      extra: <String, String?>{
        'isoDate': reportIsoDate(DateTime.now()),
        'drawLabel': localizedCashierDrawLabel(draw, translations),
        'providerSchedule': _scheduleValue(
          draw.providerDate,
          draw.providerTime,
        ),
        'providerZone': _zoneLabel(draw.providerTimezone),
        'localSchedule': _scheduleValue(draw.localDate, draw.localTime),
        'localZone': _zoneLabel(draw.localTimezone),
      },
    );
  }

  void _sellDraw(CashierAvailableDrawView draw) {
    context.push('/sell', extra: {'drawId': draw.drawId});
  }

  @override
  Widget build(BuildContext context) {
    final statsAsync = ref.watch(terminalDailyStatsProvider);
    final drawsAsync = ref.watch(availableDrawsProvider);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: _PosAppBar(
        terminalLabel: widget.home.operationalContext?.sellerTerminalLabel,
      ),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 0),
      body: SafeArea(
        top: false,
        child: RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(
              TchSpacing.s16,
              TchSpacing.s12,
              TchSpacing.s16,
              TchSpacing.s32,
            ),
            children: [
              const _ReadinessBanner(),
              const Divider(height: 1),
              const SizedBox(height: TchSpacing.s16),
              SizedBox(
                height: 48,
                child: FilledButton.icon(
                  onPressed: () => context.push('/pos/scan'),
                  icon: const Icon(Icons.qr_code_scanner_rounded),
                  label: Text(
                    translations.translate('pos.dashboard.verify_ticket'),
                  ),
                ),
              ),
              const SizedBox(height: TchSpacing.s16),
              // The summary is a fixed-height glance; the draw list is the long
              // scroller. On a 720 dp POS terminal, keeping the summary below
              // the list put it out of reach behind up to 20 draw cards.
              _SectionLabel(
                label: translations.translate('pos.dashboard.daily_summary'),
              ),
              const SizedBox(height: TchSpacing.s8),
              statsAsync.when(
                loading: () => _DailySummary.placeholder(translations),
                error: (_, _) => _DailySummary.placeholder(translations),
                data: (stats) => !stats.available
                    ? _AnalyticsUnavailable(translations: translations)
                    : stats.ticketCount == 0 && stats.salesTotalCents == 0
                    ? _NoSalesYet(translations: translations)
                    : _DailySummary(stats: stats, translations: translations),
              ),
              const SizedBox(height: TchSpacing.s24),
              _SectionLabel(
                label: translations.translate('pos.dashboard.available_draws'),
              ),
              const SizedBox(height: TchSpacing.s8),
              drawsAsync.when(
                loading: () => const Padding(
                  padding: EdgeInsets.symmetric(vertical: TchSpacing.s32),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (_, _) => _DrawsError(
                  translations: translations,
                  onRetry: () => ref.invalidate(availableDrawsProvider),
                ),
                data: (draws) => draws.isEmpty
                    ? _NoDraws(translations: translations)
                    : _FilteredAvailableDraws(
                        draws: draws,
                        selectedProvider: _selectedProvider,
                        providerFilterExpanded: _providerFilterExpanded,
                        translations: translations,
                        onFilterExpandedChanged: (expanded) =>
                            setState(() => _providerFilterExpanded = expanded),
                        onProviderChanged: (provider) =>
                            setState(() => _selectedProvider = provider),
                        onDrawTap: _openDrawReport,
                        onDrawSell: _sellDraw,
                      ),
              ),
              const SizedBox(height: TchSpacing.s32),
              _SectionLabel(
                label: translations.translate('pos.dashboard.last_ticket'),
              ),
              const SizedBox(height: TchSpacing.s8),
              ref
                  .watch(latestTicketProvider)
                  .when(
                    loading: () => const _LastTicketCard.loading(),
                    error: (_, _) => FeedbackState(
                      kind: FeedbackStateKind.error,
                      title: translations.translate(
                        'pos.dashboard.last_ticket_error',
                      ),
                      compact: true,
                    ),
                    data: (ticket) => ticket == null
                        ? FeedbackState(
                            kind: FeedbackStateKind.empty,
                            title: translations.translate(
                              'pos.dashboard.last_ticket_empty',
                            ),
                            compact: true,
                          )
                        : _LastTicketCard(
                            ticket: ticket,
                            translations: translations,
                            onReprint: () =>
                                requestTicketReprint(context, ref, ticket.id),
                            onOpen: () =>
                                context.push('/pos/tickets/${ticket.id}'),
                          ),
                  ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) => Text(
    label,
    style: Theme.of(context).textTheme.labelLarge?.copyWith(
      color: Theme.of(context).colorScheme.onSurfaceVariant,
      fontWeight: FontWeight.w800,
      letterSpacing: 0.4,
    ),
  );
}

class _DailySummary extends StatelessWidget {
  const _DailySummary({required this.stats, required this.translations})
    : isPlaceholder = false;

  const _DailySummary.placeholder(this.translations)
    : stats = null,
      isPlaceholder = true;

  final TerminalDailyStats? stats;
  final I18nBundle translations;
  final bool isPlaceholder;

  @override
  Widget build(BuildContext context) {
    final dailyStats = stats;
    return Row(
      children: [
        Expanded(
          child: _DailyMetric(
            icon: Icons.payments_outlined,
            label: translations.translate('common.cashier_stats.sales_today'),
            value: dailyStats == null
                ? '—'
                : _money(dailyStats.salesTotalCents),
            unit: dailyStats?.currency,
          ),
        ),
        const SizedBox(width: TchSpacing.s8),
        Expanded(
          child: _DailyMetric(
            icon: Icons.confirmation_number_outlined,
            label: translations.translate('common.cashier_stats.tickets'),
            value: dailyStats?.ticketCount.toString() ?? '—',
          ),
        ),
        const SizedBox(width: TchSpacing.s8),
        Expanded(
          child: _DailyMetric(
            icon: Icons.percent_rounded,
            label: translations.translate(
              'common.cashier_stats.commission_today',
            ),
            value: dailyStats == null
                ? '—'
                : _money(dailyStats.sellerCommissionTotalCents),
            unit: dailyStats?.currency,
          ),
        ),
      ],
    );
  }

  String _money(int minorUnits) => (minorUnits / 100).toStringAsFixed(2);
}

class _AnalyticsUnavailable extends StatelessWidget {
  const _AnalyticsUnavailable({required this.translations});

  final I18nBundle translations;

  @override
  Widget build(BuildContext context) => SurfaceCard(
    child: Padding(
      padding: const EdgeInsets.all(TchSpacing.s16),
      child: Row(
        children: [
          Icon(
            Icons.info_outline,
            color: Theme.of(context).colorScheme.onSurfaceVariant,
          ),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Text(
              translations.translate('pos.reports.analytics_unavailable'),
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    ),
  );
}

class _DailyMetric extends StatelessWidget {
  const _DailyMetric({
    required this.icon,
    required this.label,
    required this.value,
    this.unit,
  });

  final IconData icon;
  final String label;
  final String value;
  final String? unit;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return SurfaceCard(
      padding: const EdgeInsets.all(TchSpacing.s12),
      child: SizedBox(
        height: 96,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 18, color: scheme.primary),
            const SizedBox(height: TchSpacing.s8),
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: textTheme.labelSmall?.copyWith(
                color: scheme.onSurfaceVariant,
                fontWeight: FontWeight.w700,
              ),
            ),
            const Spacer(),
            FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Text(
                value,
                style: textTheme.headlineSmall?.copyWith(
                  color: scheme.primary,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            if (unit != null)
              Text(
                unit!,
                style: textTheme.labelSmall?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _NoSalesYet extends StatelessWidget {
  const _NoSalesYet({required this.translations});

  final I18nBundle translations;

  @override
  Widget build(BuildContext context) => SurfaceCard(
    emphasis: SurfaceCardEmphasis.low,
    padding: const EdgeInsets.all(TchSpacing.s12),
    child: Row(
      children: [
        Icon(
          Icons.point_of_sale_outlined,
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
        const SizedBox(width: TchSpacing.s8),
        Expanded(
          child: Text(
            translations.translate('pos.dashboard.no_sales_yet'),
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
            ),
          ),
        ),
      ],
    ),
  );
}

class _FilteredAvailableDraws extends StatelessWidget {
  const _FilteredAvailableDraws({
    required this.draws,
    required this.selectedProvider,
    required this.providerFilterExpanded,
    required this.translations,
    required this.onFilterExpandedChanged,
    required this.onProviderChanged,
    required this.onDrawTap,
    required this.onDrawSell,
  });

  final List<CashierAvailableDrawView> draws;
  final String? selectedProvider;
  final bool providerFilterExpanded;
  final I18nBundle translations;
  final ValueChanged<bool> onFilterExpandedChanged;
  final ValueChanged<String?> onProviderChanged;
  final ValueChanged<CashierAvailableDrawView> onDrawTap;
  final ValueChanged<CashierAvailableDrawView> onDrawSell;

  @override
  Widget build(BuildContext context) {
    final providers = draws.map((draw) => draw.providerCode).toSet().toList()
      ..sort();
    final filteredDraws = selectedProvider == null
        ? draws
        : draws
              .where((draw) => draw.providerCode == selectedProvider)
              .toList(growable: false);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (providers.length > 1) ...[
          _AvailableDrawProviderFilter(
            providers: providers,
            selectedProvider: selectedProvider,
            expanded: providerFilterExpanded,
            translations: translations,
            onExpandedChanged: onFilterExpandedChanged,
            onProviderChanged: onProviderChanged,
          ),
          const SizedBox(height: TchSpacing.s12),
        ],
        _AvailableDrawList(
          draws: filteredDraws,
          translations: translations,
          onDrawTap: onDrawTap,
          onDrawSell: onDrawSell,
        ),
      ],
    );
  }
}

class _AvailableDrawProviderFilter extends StatelessWidget {
  const _AvailableDrawProviderFilter({
    required this.providers,
    required this.selectedProvider,
    required this.expanded,
    required this.translations,
    required this.onExpandedChanged,
    required this.onProviderChanged,
  });

  final List<String> providers;
  final String? selectedProvider;
  final bool expanded;
  final I18nBundle translations;
  final ValueChanged<bool> onExpandedChanged;
  final ValueChanged<String?> onProviderChanged;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextButton.icon(
          onPressed: () => onExpandedChanged(!expanded),
          icon: const Icon(Icons.tune_rounded),
          label: Text(translations.translate('pos.dashboard.filter_draws')),
          style: TextButton.styleFrom(
            alignment: Alignment.centerLeft,
            foregroundColor: scheme.onSurfaceVariant,
          ),
        ),
        if (expanded) ...[
          const SizedBox(height: TchSpacing.s8),
          Wrap(
            spacing: TchSpacing.s8,
            runSpacing: TchSpacing.s8,
            children: [
              FilterChip(
                label: Text(
                  translations.translate('pos.dashboard.all_providers'),
                ),
                selected: selectedProvider == null,
                onSelected: (_) => onProviderChanged(null),
              ),
              for (final provider in providers)
                FilterChip(
                  label: Text(
                    '$provider · ${localizedCashierProviderLabel(provider, translations)}',
                  ),
                  selected: selectedProvider == provider,
                  onSelected: (_) => onProviderChanged(
                    selectedProvider == provider ? null : provider,
                  ),
                ),
            ],
          ),
        ],
      ],
    );
  }
}

class _AvailableDrawList extends StatelessWidget {
  const _AvailableDrawList({
    required this.draws,
    required this.translations,
    required this.onDrawTap,
    required this.onDrawSell,
  });

  final List<CashierAvailableDrawView> draws;
  final I18nBundle translations;
  final ValueChanged<CashierAvailableDrawView> onDrawTap;
  final ValueChanged<CashierAvailableDrawView> onDrawSell;

  @override
  Widget build(BuildContext context) => Column(
    children: [
      for (final draw in draws) ...[
        _AvailableDrawCard(
          draw: draw,
          translations: translations,
          onTap: () => onDrawTap(draw),
          onSell: () => onDrawSell(draw),
        ),
        const SizedBox(height: TchSpacing.s12),
      ],
    ],
  );
}

class _AvailableDrawCard extends StatelessWidget {
  const _AvailableDrawCard({
    required this.draw,
    required this.translations,
    required this.onTap,
    required this.onSell,
  });

  final CashierAvailableDrawView draw;
  final I18nBundle translations;

  /// Tapping the card body opens the draw report.
  final VoidCallback onTap;

  /// The sell button is shown only while the draw is still open.
  final VoidCallback onSell;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final now = DateTime.now();
    final urgency = _drawUrgencyFor(draw, now);

    return SurfaceCard(
      onTap: onTap,
      backgroundColor: urgency.surfaceColor(scheme),
      borderColor: urgency.borderColor(scheme),
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s12,
        vertical: TchSpacing.s8,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          TchProviderLogo(providerCode: draw.providerCode),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  localizedCashierDrawLabel(draw, translations),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: TchSpacing.s4),
                _DrawCountdown(
                  cutoffAt: draw.cutoffAt,
                  translations: translations,
                  color: urgency.foregroundColor(scheme),
                ),
              ],
            ),
          ),
          // Affordance for the card-body tap: a bare tappable card gives the
          // seller no hint that a report is one tap away. Kept as an icon so
          // the sell button stays the only labelled action on the row.
          Icon(
            Icons.chevron_right_rounded,
            size: 18,
            color: scheme.onSurfaceVariant,
          ),
          if (draw.isOpen) ...[
            const SizedBox(width: TchSpacing.s4),
            FilledButton.icon(
              key: ValueKey('draw-sell-action:${draw.drawId}'),
              onPressed: onSell,
              icon: const Icon(Icons.add_rounded, size: 18),
              label: Text(translations.translate('pos.dashboard.sell')),
              style: FilledButton.styleFrom(
                // 48 dp on a phone, 56 dp on a POS terminal. This is the most
                // tapped control in the app and it was hardcoded to 44, below
                // both figures.
                minimumSize: Size(0, context.minTouchTarget),
                padding: const EdgeInsets.symmetric(horizontal: TchSpacing.s12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(TchRadius.sm),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _DrawCountdown extends StatefulWidget {
  const _DrawCountdown({
    required this.cutoffAt,
    required this.translations,
    required this.color,
  });

  final DateTime? cutoffAt;
  final I18nBundle translations;
  final Color color;

  @override
  State<_DrawCountdown> createState() => _DrawCountdownState();
}

class _DrawCountdownState extends State<_DrawCountdown> {
  Timer? _timer;
  DateTime _now = DateTime.now();

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() => _now = DateTime.now());
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Text(
    _cutoffLabelFromDate(widget.cutoffAt, _now, widget.translations),
    style: Theme.of(context).textTheme.bodySmall?.copyWith(
      color: widget.color,
      fontWeight: FontWeight.w800,
    ),
  );
}

enum _DrawUrgency { ready, soon, urgent, closed }

_DrawUrgency _drawUrgencyFor(CashierAvailableDrawView draw, DateTime now) {
  final cutoffAt = draw.cutoffAt;
  if (cutoffAt == null) return _DrawUrgency.ready;
  final remaining = cutoffAt.difference(now);
  if (remaining.isNegative || remaining == Duration.zero) {
    return _DrawUrgency.closed;
  }
  if (remaining.inMinutes < 10) return _DrawUrgency.urgent;
  if (remaining.inMinutes < 30) return _DrawUrgency.soon;
  return _DrawUrgency.ready;
}

String _cutoffLabelFromDate(
  DateTime? cutoffAt,
  DateTime now,
  I18nBundle translations,
) {
  if (cutoffAt == null) {
    return translations.translate('pos.dashboard.closes_soon');
  }

  final remaining = cutoffAt.difference(now);
  if (remaining.isNegative || remaining == Duration.zero) {
    return translations.translate('pos.dashboard.closed');
  }
  if (remaining.inHours > 0) {
    return _interpolate(
      translations.translate('pos.dashboard.closes_in_hours_minutes'),
      {
        'hours': remaining.inHours,
        'minutes': remaining.inMinutes.remainder(60),
      },
    );
  }
  if (remaining.inMinutes > 0) {
    return _interpolate(
      translations.translate('pos.dashboard.closes_in_minutes'),
      {'minutes': remaining.inMinutes},
    );
  }
  return translations.translate('pos.dashboard.closes_soon');
}

extension on _DrawUrgency {
  Color surfaceColor(ColorScheme scheme) => switch (this) {
    _DrawUrgency.ready => TchColors.successContainer.withValues(alpha: 0.36),
    _DrawUrgency.soon => scheme.tertiaryContainer.withValues(alpha: 0.5),
    _DrawUrgency.urgent => scheme.errorContainer.withValues(alpha: 0.5),
    _DrawUrgency.closed => scheme.surfaceContainerHigh,
  };

  Color borderColor(ColorScheme scheme) => switch (this) {
    _DrawUrgency.ready => TchColors.success.withValues(alpha: 0.55),
    _DrawUrgency.soon => scheme.tertiary.withValues(alpha: 0.55),
    _DrawUrgency.urgent => scheme.error.withValues(alpha: 0.55),
    _DrawUrgency.closed => scheme.outlineVariant,
  };

  Color foregroundColor(ColorScheme scheme) => switch (this) {
    _DrawUrgency.ready => TchColors.success,
    _DrawUrgency.soon => scheme.onTertiaryContainer,
    _DrawUrgency.urgent => scheme.onErrorContainer,
    _DrawUrgency.closed => scheme.onSurfaceVariant,
  };
}

class _LastTicketCard extends StatelessWidget {
  const _LastTicketCard({
    required this.ticket,
    required this.translations,
    required this.onReprint,
    required this.onOpen,
  }) : loading = false;

  const _LastTicketCard.loading()
    : ticket = null,
      translations = null,
      onReprint = null,
      onOpen = null,
      loading = true;

  final CashierTicketSummaryView? ticket;
  final I18nBundle? translations;
  final VoidCallback? onReprint;
  final VoidCallback? onOpen;
  final bool loading;

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const SurfaceCard(
        padding: EdgeInsets.all(TchSpacing.s16),
        child: SizedBox(
          height: 56,
          child: Center(child: CircularProgressIndicator()),
        ),
      );
    }
    final currentTicket = ticket!;
    final currentTranslations = translations!;
    return SurfaceCard(
      onTap: onOpen,
      padding: const EdgeInsets.all(TchSpacing.s12),
      child: Row(
        children: [
          const Icon(Icons.receipt_long_rounded),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  currentTicket.displayCode,
                  style: Theme.of(
                    context,
                  ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w800),
                ),
                Text(
                  currentTicket.formattedAmount,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          IconButton.filledTonal(
            tooltip: currentTranslations.translate('pos.dashboard.reprint'),
            onPressed: onReprint,
            icon: const Icon(Icons.print_outlined),
          ),
        ],
      ),
    );
  }
}

class _NoDraws extends StatelessWidget {
  const _NoDraws({required this.translations});

  final I18nBundle translations;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: TchSpacing.s32),
    child: Column(
      children: [
        Icon(
          Icons.event_busy_rounded,
          size: 44,
          color: Theme.of(context).colorScheme.outlineVariant,
        ),
        const SizedBox(height: TchSpacing.s12),
        Text(
          translations.translate('pos.dashboard.no_draws'),
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      ],
    ),
  );
}

class _DrawsError extends StatelessWidget {
  const _DrawsError({required this.translations, required this.onRetry});

  final I18nBundle translations;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: TchSpacing.s24),
    child: Column(
      children: [
        Text(translations.translate('pos.dashboard.draw_load_error')),
        const SizedBox(height: TchSpacing.s8),
        TextButton(
          onPressed: onRetry,
          child: Text(translations.translate('common.retry')),
        ),
      ],
    ),
  );
}

class _ReadinessBanner extends ConsumerWidget {
  const _ReadinessBanner();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final readiness = ref.watch(cashierReadinessProvider).asData?.value;
    if (readiness == null) return const SizedBox.shrink();
    final blocker = readiness.blockers.firstOrNull;
    final notice = readiness.notifications
        .where(
          (item) =>
              item.attentionLevel == CashierAttentionLevel.blocked ||
              item.attentionLevel == CashierAttentionLevel.card,
        )
        .firstOrNull;
    if (blocker == null && notice == null) return const SizedBox.shrink();

    final translations = ref.watch(i18nBundleProvider);
    final danger =
        blocker != null ||
        notice?.attentionLevel == CashierAttentionLevel.blocked;
    final color = danger
        ? Theme.of(context).colorScheme.error
        : TchColors.warning;
    final title = _interpolate(
      translations.translate(blocker?.titleKey ?? notice!.titleKey),
      blocker?.params ?? notice!.params,
    );
    final message = _interpolate(
      translations.translate(blocker?.messageKey ?? notice!.messageKey),
      blocker?.params ?? notice!.params,
    );

    return Padding(
      padding: const EdgeInsets.only(bottom: TchSpacing.s16),
      child: SurfaceCard(
        emphasis: SurfaceCardEmphasis.low,
        padding: const EdgeInsets.all(TchSpacing.s12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              danger
                  ? Icons.error_outline_rounded
                  : Icons.warning_amber_rounded,
              color: color,
            ),
            const SizedBox(width: TchSpacing.s12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                      color: color,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  if (message.isNotEmpty) ...[
                    const SizedBox(height: TchSpacing.s4),
                    Text(message),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PosAppBar extends ConsumerWidget implements PreferredSizeWidget {
  const _PosAppBar({required this.terminalLabel});

  final String? terminalLabel;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final session = ref.watch(userSessionProvider);
    return AppBar(
      title: const TchBrandMark(size: 32, showWordmark: true),
      titleSpacing: TchSpacing.s16,
      actions: [
        if (terminalLabel != null)
          Padding(
            padding: const EdgeInsets.only(right: TchSpacing.s8),
            child: _TerminalStatus(label: terminalLabel!),
          ),
        _NotificationCenterAction(
          tooltip: translations.translate('notifications.center.open'),
        ),
        Padding(
          padding: const EdgeInsets.only(right: TchSpacing.s12),
          child: _UserAvatar(
            initials: _initials(session.displayName ?? session.username),
            tooltip: translations.translate('pos.profile.open'),
            label: translations.translate('pos.dashboard.profile'),
            onTap: () => _showAccountMenu(context, ref, translations),
          ),
        ),
      ],
    );
  }
}

class _TerminalStatus extends ConsumerWidget {
  const _TerminalStatus({required this.label});

  final String label;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Text(
          label,
          overflow: TextOverflow.ellipsis,
          style: Theme.of(
            context,
          ).textTheme.labelSmall?.copyWith(fontWeight: FontWeight.w800),
        ),
        OnlineBadge(
          online: ref.watch(isOnlineProvider).asData?.value ?? true,
          onlineLabel: translations.translate('common.status.online'),
          offlineLabel: translations.translate('common.status.offline'),
        ),
      ],
    );
  }
}

class _NotificationCenterAction extends ConsumerWidget {
  const _NotificationCenterAction({required this.tooltip});

  final String tooltip;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final summary = ref.watch(notificationSummaryProvider).summary;
    final notificationSettings = ref
        .watch(posProfileProvider)
        .asData
        ?.value
        .settings
        .notifications;
    final notificationsEnabled = notificationSettings?.enabled != false;
    final criticalOnly = notificationSettings?.criticalOnly == true;
    final unreadCount = !notificationsEnabled
        ? 0
        : criticalOnly
        ? summary.criticalCount
        : summary.unreadCount;
    final criticalCount = notificationsEnabled ? summary.criticalCount : 0;
    final actionRequiredCount = notificationsEnabled && !criticalOnly
        ? summary.actionRequiredCount
        : 0;
    final scheme = Theme.of(context).colorScheme;
    final badgeColor = criticalCount > 0
        ? scheme.error
        : actionRequiredCount > 0
        ? TchColors.warning
        : scheme.primary;
    return IconButton(
      tooltip: tooltip,
      onPressed: notificationsEnabled
          ? () => context.push('/pos/notifications')
          : null,
      icon: Badge(
        isLabelVisible: unreadCount > 0,
        backgroundColor: badgeColor,
        label: Text(unreadCount > 99 ? '99+' : '$unreadCount'),
        child: const Icon(Icons.notifications_outlined),
      ),
    );
  }
}

class _UserAvatar extends StatelessWidget {
  const _UserAvatar({
    required this.initials,
    required this.tooltip,
    required this.label,
    required this.onTap,
  });

  final String initials;
  final String tooltip;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      button: true,
      label: tooltip,
      child: Tooltip(
        message: tooltip,
        child: InkWell(
          borderRadius: BorderRadius.circular(TchRadius.pill),
          onTap: onTap,
          child: SizedBox(
            width: 52,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                CircleAvatar(
                  radius: 16,
                  backgroundColor: scheme.primary,
                  child: Text(
                    initials,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: scheme.onPrimary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(height: TchSpacing.s4),
                Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(
                    context,
                  ).textTheme.labelSmall?.copyWith(fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

Future<void> _showAccountMenu(
  BuildContext context,
  WidgetRef ref,
  I18nBundle translations,
) async {
  final action = await showModalBottomSheet<_AccountMenuAction>(
    context: context,
    useSafeArea: true,
    showDragHandle: true,
    builder: (sheetContext) => SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          TchSpacing.s16,
          0,
          TchSpacing.s16,
          TchSpacing.s16,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.person_rounded),
              title: Text(translations.translate('pos.profile.title')),
              onTap: () =>
                  Navigator.of(sheetContext).pop(_AccountMenuAction.profile),
            ),
            ListTile(
              leading: const Icon(Icons.settings_rounded),
              title: Text(translations.translate('pos.profile.settings')),
              onTap: () =>
                  Navigator.of(sheetContext).pop(_AccountMenuAction.settings),
            ),
            const Divider(height: TchSpacing.s16),
            ListTile(
              leading: Icon(
                Icons.logout_rounded,
                color: Theme.of(sheetContext).colorScheme.error,
              ),
              title: Text(translations.translate('pos.profile.sign_out')),
              textColor: Theme.of(sheetContext).colorScheme.error,
              iconColor: Theme.of(sheetContext).colorScheme.error,
              onTap: () =>
                  Navigator.of(sheetContext).pop(_AccountMenuAction.logout),
            ),
          ],
        ),
      ),
    ),
  );

  if (!context.mounted || action == null) return;
  switch (action) {
    case _AccountMenuAction.profile:
      context.go('/pos/profile');
    case _AccountMenuAction.settings:
      context.go('/pos/settings');
    case _AccountMenuAction.logout:
      await showLogoutConfirmation(context, ref);
  }
}

enum _AccountMenuAction { profile, settings, logout }

String _initials(String? name) {
  if (name == null || name.trim().isEmpty) return '?';
  final parts = name.trim().split(RegExp(r'\s+'));
  return parts.length > 1
      ? '${parts.first[0]}${parts.last[0]}'.toUpperCase()
      : parts.first[0].toUpperCase();
}

String _interpolate(String value, Map<String, dynamic> params) =>
    value.replaceAllMapped(
      RegExp(r'\{(\w+)\}'),
      (match) => params[match[1]]?.toString() ?? match[0]!,
    );

String _scheduleValue(String date, String time) {
  final datePart = date.length >= 10
      ? '${date.substring(8, 10)}/${date.substring(5, 7)}'
      : date;
  final timePart = time.length >= 5 ? time.substring(0, 5) : time;
  return '$datePart $timePart'.trim();
}

String _zoneLabel(String timezone) =>
    timezone.split('/').last.replaceAll('_', ' ').replaceAll('-', ' ');
