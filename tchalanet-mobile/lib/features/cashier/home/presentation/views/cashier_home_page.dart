import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../core/network/api_exception.dart';
import '../../../../../core/network/connectivity_repository.dart';
import '../../../../../design_system/components/online_badge.dart';
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

    return homeAsync.when(
      loading: () => const _LoadingScaffold(),
      error: (e, _) => _ErrorScaffold(error: userMessage(e), ref: ref),
      data: (home) {
        // V1 SellerTerminal model: the only blocking step the server emits is
        // MUST_CHANGE_PIN. No more outlet/session selection.
        if (home.requiredStep != null) {
          if (home.mustChangePin && !_pinRedirectScheduled) {
            _pinRedirectScheduled = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (mounted) context.go('/change-pin');
            });
          }
          return _BlockedStepScaffold(home: home);
        }
        return _SellerTerminalScaffold(home: home);
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

// ─── Blocking step (V1: MUST_CHANGE_PIN only) ────────────────────────────────

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
        onMenuTap: null,
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
              FilledButton.icon(
                onPressed: () async {
                  await context.push('/change-pin');
                  ref.invalidate(cashierHomeProvider);
                },
                icon: const Icon(Icons.password_rounded),
                label: Text(translations.translate('auth.change_pin.title')),
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

// ─── SellerTerminal home ──────────────────────────────────────────────────────

class _SellerTerminalScaffold extends ConsumerWidget {
  const _SellerTerminalScaffold({required this.home});

  final CashierHomeResponse home;

  void _showDrawDetail(
    BuildContext context,
    WidgetRef ref,
    CashierAvailableDrawView draw,
  ) {
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
        terminalLabel: home.operationalContext?.sellerTerminalLabel,
        onMenuTap: null,
      ),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 0),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(cashierHomeProvider);
            ref.invalidate(cashierReadinessProvider);
            ref.invalidate(terminalDailyStatsProvider);
            ref.invalidate(availableDrawsProvider);
          },
          child: ListView(
            padding: const EdgeInsets.all(TchSpacing.s16),
            children: [
              // Readiness attention banner (blockers / urgent notices).
              const _ReadinessBanner(),
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
                              onTap: () => _showDrawDetail(context, ref, draw),
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

// ─── Readiness attention banner ───────────────────────────────────────────────

class _ReadinessBanner extends ConsumerWidget {
  const _ReadinessBanner();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final readiness = ref.watch(cashierReadinessProvider).asData?.value;
    if (readiness == null) return const SizedBox.shrink();

    // Priority: a hard blocker first, then the most urgent attention notice.
    final blocker = readiness.blockers.firstOrNull;
    final notice = readiness.notifications
        .where(
          (n) =>
              n.attentionLevel == CashierAttentionLevel.blocked ||
              n.attentionLevel == CashierAttentionLevel.card,
        )
        .firstOrNull;
    if (blocker == null && notice == null) return const SizedBox.shrink();

    final translations = ref.watch(i18nBundleProvider);
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    final danger =
        blocker != null ||
        notice?.attentionLevel == CashierAttentionLevel.blocked;
    final color = danger ? scheme.error : TchColors.warning;

    final titleKey = blocker?.titleKey ?? notice!.titleKey;
    final messageKey = blocker?.messageKey ?? notice!.messageKey;
    final params = blocker?.params ?? notice!.params;
    final title = _interpolate(translations.translate(titleKey), params);
    final message = _interpolate(translations.translate(messageKey), params);

    return Container(
      margin: const EdgeInsets.only(bottom: TchSpacing.s16),
      padding: const EdgeInsets.all(TchSpacing.s12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            danger ? Icons.error_outline_rounded : Icons.warning_amber_rounded,
            size: 20,
            color: color,
          ),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                    color: color,
                  ),
                ),
                if (message.isNotEmpty) ...[
                  const SizedBox(height: TchSpacing.s4),
                  Text(
                    message,
                    style: textTheme.bodySmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Fills `{name}` placeholders from server params (the i18n bundle has no
  /// interpolation of its own).
  static String _interpolate(String value, Map<String, dynamic> params) =>
      value.replaceAllMapped(
        RegExp(r'\{(\w+)\}'),
        (m) => params[m[1]]?.toString() ?? m[0]!,
      );
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
          child: StatCardLarge(
            label: "Ventes aujourd'hui",
            value: '—',
            unit: '',
          ),
        ),
        SizedBox(width: TchSpacing.s12),
        SizedBox(
          width: 100,
          child: StatCard(label: 'Tickets', value: '—'),
        ),
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
        TchSpacing.s24,
        TchSpacing.s8,
        TchSpacing.s24,
        TchSpacing.s32,
      ),
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
                    style: textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    draw.formattedCutoff,
                    style: textTheme.bodySmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: TchSpacing.s8,
                  vertical: TchSpacing.s4,
                ),
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
              style: textTheme.bodyMedium?.copyWith(
                color: scheme.onSurfaceVariant,
              ),
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
    final slotFg = isMidi
        ? scheme.onPrimaryContainer
        : scheme.onSecondaryContainer;

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
              style: textTheme.bodySmall?.copyWith(
                color: scheme.onSurfaceVariant,
              ),
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
          Icon(
            Icons.event_busy_rounded,
            size: 48,
            color: scheme.outlineVariant,
          ),
          const SizedBox(height: TchSpacing.s12),
          Text(
            'Aucun tirage disponible',
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: scheme.onSurfaceVariant),
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
          Text(
            'Impossible de charger les tirages',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: TchSpacing.s12),
          TextButton(onPressed: onRetry, child: const Text('Réessayer')),
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
                  online: ref.watch(isOnlineProvider).asData?.value ?? true,
                  onlineLabel: translations.translate('common.status.online'),
                  offlineLabel: translations.translate('common.status.offline'),
                ),
              ],
            ),
          ),
        Builder(
          builder: (context) {
            final summary = ref.watch(notificationSummaryProvider).summary;
            return _NotificationCenterAction(
              unreadCount: summary.unreadCount,
              criticalCount: summary.criticalCount,
              actionRequiredCount: summary.actionRequiredCount,
              tooltip: translations.translate('notifications.center.open'),
            );
          },
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
    required this.criticalCount,
    required this.actionRequiredCount,
    required this.tooltip,
  });

  final int unreadCount;
  final int criticalCount;
  final int actionRequiredCount;
  final String tooltip;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    // Badge count is the unread total; its colour signals the highest severity
    // present so the seller reads urgency at a glance (matches the design).
    final Color badgeColor = criticalCount > 0
        ? scheme.error
        : actionRequiredCount > 0
        ? TchColors.warning
        : scheme.primary;

    return IconButton(
      tooltip: tooltip,
      onPressed: () => context.push('/pos/notifications'),
      icon: Badge(
        isLabelVisible: unreadCount > 0,
        backgroundColor: badgeColor,
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
