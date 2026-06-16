import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../design_system/components/online_badge.dart';
import '../../../../../design_system/components/pos_action_button.dart';
import '../../../../../design_system/components/pos_bottom_nav_bar.dart';
import '../../../../../design_system/components/stat_card.dart';
import '../../../../../design_system/tokens/tch_colors.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../../../../notifications/presentation/view_models/notification_summary_controller.dart';
import '../../../tickets/data/models/cashier_sell_catalog_models.dart';
import '../../data/models/cashier_home_models.dart';
import '../../data/services/terminal_stats_service.dart';
import '../view_models/cashier_home_providers.dart';
import 'seller_terminal_nav_bar.dart';

class CashierHomePage extends ConsumerWidget {
  const CashierHomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homeAsync = ref.watch(cashierHomeProvider);

    return homeAsync.when(
      loading: () => const _LoadingScaffold(),
      error: (e, _) => _ErrorScaffold(error: e.toString(), ref: ref),
      data: (home) {
        if (home.needsOpContext) {
          return _SetupRequiredScaffold(home: home);
        }
        if (home.needsSession) {
          return _SessionClosedScaffold(home: home);
        }
        return _OperationalScaffold(home: home);
      },
    );
  }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

class _LoadingScaffold extends StatelessWidget {
  const _LoadingScaffold();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      appBar: _PosAppBar(terminalLabel: null, onMenuTap: null),
      body: Center(child: CircularProgressIndicator()),
    );
  }
}

// ─── Error ────────────────────────────────────────────────────────────────────

class _ErrorScaffold extends StatelessWidget {
  const _ErrorScaffold({required this.error, required this.ref});

  final String error;
  final WidgetRef ref;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: const _PosAppBar(terminalLabel: null, onMenuTap: null),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(TchSpacing.s24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
              const SizedBox(height: TchSpacing.s16),
              Text(
                'Impossible de charger le tableau de bord',
                style: Theme.of(context).textTheme.titleMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: TchSpacing.s24),
              FilledButton.tonal(
                onPressed: () => ref.invalidate(cashierHomeProvider),
                child: const Text('Réessayer'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── State 1: setup required (no operational context) ────────────────────────

class _SetupRequiredScaffold extends ConsumerWidget {
  const _SetupRequiredScaffold({required this.home});

  final CashierHomeResponse home;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final step = home.requiredStep!;

    return Scaffold(
      appBar: _PosAppBar(terminalLabel: home.header?.subtitle, onMenuTap: null),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(TchSpacing.s24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              Icon(
                Icons.settings_input_component_rounded,
                size: 64,
                color: scheme.primary,
              ),
              const SizedBox(height: TchSpacing.s24),
              Text(
                step.title,
                style: textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: TchSpacing.s12),
              Text(
                step.message,
                style: textTheme.bodyMedium?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(),
              FilledButton.icon(
                onPressed: () => context.push('/pos/setup'),
                icon: const Icon(Icons.tune_rounded),
                label: Text(home.primaryAction?.label ?? 'Configurer le poste'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(56),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                ),
              ),
              const SizedBox(height: TchSpacing.s16),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── State 2: session closed ──────────────────────────────────────────────────

class _SessionClosedScaffold extends ConsumerWidget {
  const _SessionClosedScaffold({required this.home});

  final CashierHomeResponse home;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final step = home.requiredStep!;

    return Scaffold(
      appBar: _PosAppBar(
        terminalLabel: home.operationalContext != null
            ? _subtitle(home.operationalContext!)
            : home.header?.subtitle,
        onMenuTap: null,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(TchSpacing.s24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              Icon(Icons.lock_clock_rounded, size: 64, color: scheme.secondary),
              const SizedBox(height: TchSpacing.s24),
              Text(
                step.title,
                style: textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: TchSpacing.s12),
              Text(
                step.message,
                style: textTheme.bodyMedium?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),
              const Spacer(),
              FilledButton.icon(
                onPressed: () => context.push('/pos/session/open'),
                icon: const Icon(Icons.play_circle_outline_rounded),
                label: Text(home.primaryAction?.label ?? 'Ouvrir session'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(56),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(TchRadius.md),
                  ),
                ),
              ),
              const SizedBox(height: TchSpacing.s16),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── State 3: operational home ────────────────────────────────────────────────

class _OperationalScaffold extends ConsumerWidget {
  const _OperationalScaffold({required this.home});

  final CashierHomeResponse home;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isSellerTerminal =
        home.operationalContext?.source == 'SELLER_TERMINAL';

    if (isSellerTerminal) {
      return _SellerTerminalScaffold(home: home);
    }

    final session = home.session;
    final primaryAction = home.primaryAction;
    final quickActions = home.quickActions;
    final payoutWidget = home.widgets
        .where((w) => w.type == 'POS_PAYOUT_STATUS')
        .firstOrNull;

    return Scaffold(
      appBar: _PosAppBar(
        terminalLabel: home.operationalContext != null
            ? _subtitle(home.operationalContext!)
            : null,
        onMenuTap: null,
      ),
      bottomNavigationBar: const PosBottomNavBar(currentIndex: 0),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(
            TchSpacing.s16,
            TchSpacing.s16,
            TchSpacing.s16,
            TchSpacing.s8,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (primaryAction != null)
                PosActionButton(
                  label: primaryAction.label,
                  icon: Icons.confirmation_number_rounded,
                  size: PosActionButtonSize.large,
                  enabled: primaryAction.enabled,
                  onPressed: primaryAction.enabled
                      ? () => context.push(primaryAction.route)
                      : null,
                ),
              const SizedBox(height: TchSpacing.s16),
              if (quickActions.isNotEmpty)
                _QuickActionsGrid(actions: quickActions),
              const SizedBox(height: TchSpacing.s12),
              _SyncButton(onPressed: () => ref.invalidate(cashierHomeProvider)),
              const SizedBox(height: TchSpacing.s24),
              if (session != null) ...[
                StatCardLarge(
                  label: "Ventes Aujourd'hui",
                  value: session.salesTotal?.split(' ').first ?? '0.00',
                  unit: session.salesTotal?.contains(' ') == true
                      ? session.salesTotal!.split(' ').last
                      : 'HTG',
                ),
                const SizedBox(height: TchSpacing.s12),
                Row(
                  children: [
                    Expanded(
                      child: StatCard(
                        label: 'Tickets',
                        value: session.ticketCount.toString(),
                      ),
                    ),
                    if (payoutWidget != null) ...[
                      const SizedBox(width: TchSpacing.s12),
                      Expanded(
                        child: StatCard(
                          label: payoutWidget.title ?? 'Gagnants',
                          value: payoutWidget.data['total']?.toString() ?? '—',
                          accentColor: Theme.of(context).colorScheme.tertiary,
                        ),
                      ),
                    ],
                  ],
                ),
              ],
              if (session?.openedAtLabel != null) ...[
                const SizedBox(height: TchSpacing.s12),
                _SessionInfoRow(session: session!),
              ],
              const SizedBox(height: TchSpacing.s8),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── SellerTerminal home ──────────────────────────────────────────────────────

class _SellerTerminalScaffold extends ConsumerWidget {
  const _SellerTerminalScaffold({required this.home});

  final CashierHomeResponse home;

  void _showDrawDetail(
      BuildContext context, WidgetRef ref, CashierAvailableDrawView draw) {
    final statsAsync = ref.read(terminalDailyStatsProvider);
    final drawLine = statsAsync.asData?.value.breakdown
        .where((b) => b.drawId == draw.drawId)
        .firstOrNull;
    showModalBottomSheet<void>(
      context: context,
      useSafeArea: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(TchRadius.lg)),
      ),
      builder: (sheetCtx) => _DrawDetailSheet(
        draw: draw,
        stat: drawLine,
        onSell: () {
          Navigator.of(sheetCtx).pop();
          context.push('/sell', extra: {'drawId': draw.drawId});
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final statsAsync = ref.watch(terminalDailyStatsProvider);
    final drawsAsync = ref.watch(availableDrawsProvider);

    return Scaffold(
      appBar: _PosAppBar(
        terminalLabel: home.operationalContext?.terminalLabel,
        onMenuTap: null,
      ),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 0),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(cashierHomeProvider);
            ref.invalidate(terminalDailyStatsProvider);
            ref.invalidate(availableDrawsProvider);
          },
          child: ListView(
            padding: const EdgeInsets.all(TchSpacing.s16),
            children: [
              // Stats
              statsAsync.when(
                loading: () => const _StatsPlaceholder(),
                error: (_, _) => const _StatsPlaceholder(),
                data: (stats) => _TerminalStatsRow(stats: stats),
              ),
              const SizedBox(height: TchSpacing.s24),

              // Draws header
              Text(
                'TIRAGES DISPONIBLES',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                  letterSpacing: 0.5,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: TchSpacing.s12),

              // Draw list
              drawsAsync.when(
                loading: () => const Center(
                  child: Padding(
                    padding: EdgeInsets.all(TchSpacing.s24),
                    child: CircularProgressIndicator(),
                  ),
                ),
                error: (e, _) => _DrawsError(
                  onRetry: () => ref.invalidate(availableDrawsProvider),
                ),
                data: (draws) => draws.isEmpty
                    ? _NoDraws()
                    : GridView.count(
                        crossAxisCount: 2,
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisSpacing: TchSpacing.s12,
                        mainAxisSpacing: TchSpacing.s12,
                        childAspectRatio: 1.25,
                        children: [
                          for (final draw in draws)
                            _DrawTile(
                              draw: draw,
                              onTap: () => _showDrawDetail(
                                context,
                                ref,
                                draw,
                              ),
                            ),
                        ],
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TerminalStatsRow extends StatelessWidget {
  const _TerminalStatsRow({required this.stats});

  final TerminalDailyStats stats;

  @override
  Widget build(BuildContext context) {
    final amount = (stats.salesTotalCents / 100.0).toStringAsFixed(2);
    return Row(
      children: [
        Expanded(
          child: StatCardLarge(
            label: "Ventes aujourd'hui",
            value: amount,
            unit: stats.currency,
          ),
        ),
        const SizedBox(width: TchSpacing.s12),
        SizedBox(
          width: 100,
          child: StatCard(
            label: 'Tickets',
            value: stats.ticketCount.toString(),
          ),
        ),
      ],
    );
  }
}

class _StatsPlaceholder extends StatelessWidget {
  const _StatsPlaceholder();

  @override
  Widget build(BuildContext context) {
    return const Row(
      children: [
        Expanded(
          child: StatCardLarge(label: "Ventes aujourd'hui", value: '—', unit: ''),
        ),
        SizedBox(width: TchSpacing.s12),
        SizedBox(width: 100, child: StatCard(label: 'Tickets', value: '—')),
      ],
    );
  }
}

// ─── Draw detail bottom sheet ─────────────────────────────────────────────────

class _DrawDetailSheet extends StatelessWidget {
  const _DrawDetailSheet({
    required this.draw,
    required this.stat,
    required this.onSell,
  });

  final CashierAvailableDrawView draw;
  final DrawStatLine? stat;
  final VoidCallback onSell;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final parts = draw.channelLabel.split(' ');
    final stateCode = parts[0];
    final slot = parts.length > 1 ? parts.sublist(1).join(' ') : '';

    return Padding(
      padding: const EdgeInsets.fromLTRB(
          TchSpacing.s24, TchSpacing.s8, TchSpacing.s24, TchSpacing.s32),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Handle
          Container(
            width: 40,
            height: 4,
            margin: const EdgeInsets.only(bottom: TchSpacing.s24),
            decoration: BoxDecoration(
              color: scheme.outlineVariant,
              borderRadius: BorderRadius.circular(TchRadius.pill),
            ),
          ),

          // Draw identity
          Row(
            children: [
              Text(
                stateCode,
                style: textTheme.displaySmall?.copyWith(
                  fontWeight: FontWeight.w900,
                  color: scheme.onSurface,
                  height: 1,
                ),
              ),
              const SizedBox(width: TchSpacing.s12),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    slot,
                    style: textTheme.titleLarge
                        ?.copyWith(fontWeight: FontWeight.w700),
                  ),
                  Text(
                    draw.formattedCutoff,
                    style: textTheme.bodySmall
                        ?.copyWith(color: scheme.onSurfaceVariant),
                  ),
                ],
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(
                    horizontal: TchSpacing.s8, vertical: TchSpacing.s4),
                decoration: BoxDecoration(
                  color: TchColors.successContainer,
                  borderRadius: BorderRadius.circular(TchRadius.pill),
                ),
                child: Text(
                  'OUVERT',
                  style: textTheme.labelSmall?.copyWith(
                    color: TchColors.success,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: TchSpacing.s24),
          const Divider(),
          const SizedBox(height: TchSpacing.s16),

          // Stats for this draw today
          if (stat != null)
            Row(
              children: [
                Expanded(
                  child: StatCardLarge(
                    label: 'Ventes ce tirage',
                    value: stat!.totalAmount.toStringAsFixed(2),
                    unit: '',
                  ),
                ),
                const SizedBox(width: TchSpacing.s12),
                SizedBox(
                  width: 100,
                  child: StatCard(
                    label: 'Tickets',
                    value: stat!.ticketCount.toString(),
                  ),
                ),
              ],
            )
          else
            Text(
              'Aucune vente pour ce tirage aujourd\'hui',
              style: textTheme.bodyMedium
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),

          const SizedBox(height: TchSpacing.s24),

          // Vendre button
          FilledButton.icon(
            onPressed: onSell,
            icon: const Icon(Icons.confirmation_number_rounded),
            label: const Text('VENDRE'),
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              textStyle: textTheme.labelLarge?.copyWith(
                fontWeight: FontWeight.w800,
                letterSpacing: 1.5,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(TchRadius.md),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Draw tile (grid cell) ────────────────────────────────────────────────────

class _DrawTile extends StatelessWidget {
  const _DrawTile({required this.draw, required this.onTap});

  final CashierAvailableDrawView draw;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    // "NY Midi" → stateCode="NY", slot="Midi"
    final parts = draw.channelLabel.split(' ');
    final stateCode = parts[0];
    final slot = parts.length > 1 ? parts.sublist(1).join(' ') : '';
    final isMidi = slot.toLowerCase().contains('midi');

    final slotBg = isMidi ? scheme.primaryContainer : scheme.secondaryContainer;
    final slotFg =
        isMidi ? scheme.onPrimaryContainer : scheme.onSecondaryContainer;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(TchRadius.md),
      child: Container(
        decoration: BoxDecoration(
          color: scheme.surfaceContainerLow,
          borderRadius: BorderRadius.circular(TchRadius.md),
          border: Border.all(
            color: TchColors.success.withValues(alpha: 0.35),
            width: 1.5,
          ),
        ),
        padding: const EdgeInsets.symmetric(
          horizontal: TchSpacing.s12,
          vertical: TchSpacing.s16,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              stateCode,
              style: textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.w900,
                color: scheme.onSurface,
                height: 1,
              ),
            ),
            const SizedBox(height: TchSpacing.s8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
              decoration: BoxDecoration(
                color: slotBg,
                borderRadius: BorderRadius.circular(TchRadius.pill),
              ),
              child: Text(
                slot,
                style: textTheme.labelMedium?.copyWith(
                  color: slotFg,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            const SizedBox(height: TchSpacing.s8),
            Text(
              draw.formattedCutoff,
              style: textTheme.bodySmall
                  ?.copyWith(color: scheme.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

class _NoDraws extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: TchSpacing.s32),
      child: Column(
        children: [
          Icon(Icons.event_busy_rounded, size: 48, color: scheme.outlineVariant),
          const SizedBox(height: TchSpacing.s12),
          Text(
            'Aucun tirage disponible',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _DrawsError extends StatelessWidget {
  const _DrawsError({required this.onRetry});

  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        children: [
          const SizedBox(height: TchSpacing.s16),
          Text('Impossible de charger les tirages',
              style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: TchSpacing.s12),
          TextButton(onPressed: onRetry, child: const Text('Réessayer')),
        ],
      ),
    );
  }
}

// ─── Quick actions grid ───────────────────────────────────────────────────────

class _QuickActionsGrid extends StatelessWidget {
  const _QuickActionsGrid({required this.actions});

  final List<HomeAction> actions;

  static const _icons = {
    'VERIFY_TICKET': Icons.fact_check_rounded,
    'PAY_WINNER': Icons.payments_rounded,
    'RECENT_TICKETS': Icons.receipt_long_rounded,
    'SESSION': Icons.timer_rounded,
    'PROFILE': Icons.person_rounded,
  };

  @override
  Widget build(BuildContext context) {
    final visible = actions.take(2).toList(); // max 2 in grid
    if (visible.isEmpty) return const SizedBox.shrink();

    return Row(
      children: [
        for (int i = 0; i < visible.length; i++) ...[
          if (i > 0) const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Builder(
              builder: (context) {
                final a = visible[i];
                return PosActionButton(
                  label: a.label,
                  icon: _icons[a.type] ?? Icons.touch_app_rounded,
                  tone: PosActionButtonTone.secondary,
                  size: PosActionButtonSize.medium,
                  enabled: a.enabled,
                  onPressed: a.enabled ? () => context.push(a.route) : null,
                );
              },
            ),
          ),
        ],
      ],
    );
  }
}

// ─── Sync button ──────────────────────────────────────────────────────────────

class _SyncButton extends StatelessWidget {
  const _SyncButton({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SizedBox(
      height: 56,
      child: OutlinedButton.icon(
        onPressed: onPressed,
        icon: Icon(Icons.sync_rounded, color: scheme.primary),
        label: Text(
          'ACTUALISER',
          style: Theme.of(context).textTheme.labelLarge?.copyWith(
            color: scheme.primary,
            fontWeight: FontWeight.w700,
            letterSpacing: 1.5,
          ),
        ),
        style: OutlinedButton.styleFrom(
          side: BorderSide(color: scheme.outlineVariant),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(TchRadius.md),
          ),
        ),
      ),
    );
  }
}

// ─── Session info row ─────────────────────────────────────────────────────────

class _SessionInfoRow extends StatelessWidget {
  const _SessionInfoRow({required this.session});

  final CashierHomeSession session;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s16,
        vertical: TchSpacing.s12,
      ),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(TchRadius.md),
      ),
      child: Row(
        children: [
          Icon(Icons.access_time_rounded, size: 16, color: scheme.outline),
          const SizedBox(width: TchSpacing.s8),
          Text(
            'Session ouverte à ${session.openedAtLabel}',
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

// ─── Top app bar ──────────────────────────────────────────────────────────────

class _PosAppBar extends ConsumerWidget implements PreferredSizeWidget {
  const _PosAppBar({required this.terminalLabel, required this.onMenuTap});

  final String? terminalLabel;
  final VoidCallback? onMenuTap;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final session = ref.watch(userSessionProvider);
    final translations = ref.watch(i18nBundleProvider);

    return AppBar(
      // backgroundColor, elevation, surfaceTintColor from appBarTheme in ThemeBuilder
      leading: IconButton(
        icon: const Icon(Icons.menu_rounded),
        onPressed: onMenuTap,
        color: scheme.primary,
      ),
      title: Text(
        'Tchalanet',
        style: textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w900,
          color: scheme.primary,
          letterSpacing: -0.5,
        ),
      ),
      actions: [
        if (terminalLabel != null)
          Padding(
            padding: const EdgeInsets.only(right: TchSpacing.s8),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  terminalLabel!,
                  style: textTheme.labelSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: scheme.primary,
                  ),
                ),
                OnlineBadge(
                  online: true,
                  onlineLabel: translations.translate('common.status.online'),
                  offlineLabel: translations.translate('common.status.offline'),
                ),
              ],
            ),
          ),
        _NotificationCenterAction(
          unreadCount: ref.watch(
            notificationSummaryProvider.select(
              (state) => state.summary.unreadCount,
            ),
          ),
          tooltip: translations.translate('notifications.center.open'),
        ),
        Padding(
          padding: const EdgeInsets.only(right: TchSpacing.s12),
          child: _UserAvatar(
            initials: _initials(session.displayName ?? session.username),
          ),
        ),
      ],
    );
  }

  String _initials(String? name) {
    if (name == null || name.isEmpty) return '?';
    final parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
    }
    return name[0].toUpperCase();
  }
}

class _NotificationCenterAction extends StatelessWidget {
  const _NotificationCenterAction({
    required this.unreadCount,
    required this.tooltip,
  });

  final int unreadCount;
  final String tooltip;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: tooltip,
      onPressed: () => context.push('/pos/notifications'),
      icon: Badge(
        isLabelVisible: unreadCount > 0,
        label: Text(unreadCount > 99 ? '99+' : unreadCount.toString()),
        child: const Icon(Icons.notifications_outlined),
      ),
    );
  }
}

class _UserAvatar extends StatelessWidget {
  const _UserAvatar({required this.initials});

  final String initials;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return CircleAvatar(
      radius: 18,
      backgroundColor: scheme.primaryContainer,
      child: Text(
        initials,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          fontWeight: FontWeight.w700,
          color: scheme.onPrimaryContainer,
        ),
      ),
    );
  }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

String _subtitle(CashierHomeOpCtx ctx) {
  final outlet = ctx.outletName;
  final terminal = ctx.terminalLabel;
  if (outlet == null && terminal == null) return '';
  if (outlet == null) return terminal!;
  if (terminal == null) return outlet;
  return '$outlet • $terminal';
}
