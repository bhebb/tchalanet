import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../design_system/components/online_badge.dart';
import '../../../../../design_system/components/pos_action_button.dart';
import '../../../../../design_system/components/stat_card.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../../data/models/cashier_home_models.dart';
import '../view_models/cashier_home_providers.dart';

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
      appBar: _PosAppBar(
        terminalLabel: home.header?.subtitle,
        onMenuTap: null,
      ),
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
              Icon(
                Icons.lock_clock_rounded,
                size: 64,
                color: scheme.secondary,
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
    final session = home.session;
    final primaryAction = home.primaryAction;
    final quickActions = home.quickActions;

    final syncAction = home.widgets
        .where((w) => w.type == 'POS_SYNC')
        .firstOrNull;

    // "Gagnants" widget if server sends it (open question — graceful skip if absent)
    final payoutWidget = home.widgets
        .where((w) => w.type == 'POS_PAYOUT_STATUS')
        .firstOrNull;

    return Scaffold(
      appBar: _PosAppBar(
        terminalLabel: home.operationalContext != null
            ? _subtitle(home.operationalContext!)
            : null,
        onMenuTap: () => Scaffold.of(context).openDrawer(),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
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
                    // Primary action
                    if (primaryAction != null)
                      PosActionButton(
                        label: primaryAction.label,
                        icon: Icons.confirmation_number_rounded,
                        color: Theme.of(context).colorScheme.primary,
                        size: PosActionButtonSize.large,
                        enabled: primaryAction.enabled,
                        onPressed: primaryAction.enabled
                            ? () => context.push(primaryAction.route)
                            : null,
                      ),
                    const SizedBox(height: TchSpacing.s16),

                    // Quick actions grid
                    if (quickActions.isNotEmpty)
                      _QuickActionsGrid(actions: quickActions),
                    const SizedBox(height: TchSpacing.s12),

                    // Sync button
                    _SyncButton(
                      onPressed: syncAction != null
                          ? () {}
                          : () => ref.invalidate(cashierHomeProvider),
                    ),
                    const SizedBox(height: TchSpacing.s24),

                    // Stats
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
                                value: payoutWidget.data['total']?.toString() ??
                                    '—',
                                accentColor:
                                    Theme.of(context).colorScheme.tertiary,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ],

                    // Session info row
                    if (session?.openedAtLabel != null) ...[
                      const SizedBox(height: TchSpacing.s12),
                      _SessionInfoRow(session: session!),
                    ],
                    const SizedBox(height: TchSpacing.s8),
                  ],
                ),
              ),
            ),
            const _PosBottomNavBar(currentIndex: 0),
          ],
        ),
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
    final scheme = Theme.of(context).colorScheme;
    final visible = actions.take(2).toList(); // max 2 in grid
    if (visible.isEmpty) return const SizedBox.shrink();

    return Row(
      children: [
        for (int i = 0; i < visible.length; i++) ...[
          if (i > 0) const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Builder(builder: (context) {
              final a = visible[i];
              return PosActionButton(
                label: a.label,
                icon: _icons[a.type] ?? Icons.touch_app_rounded,
                color: scheme.secondary,
                size: PosActionButtonSize.medium,
                enabled: a.enabled,
                onPressed: a.enabled ? () => context.push(a.route) : null,
              );
            }),
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
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: scheme.onSurfaceVariant,
                ),
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

    return AppBar(
      backgroundColor: scheme.surface,
      elevation: 0,
      scrolledUnderElevation: 1,
      surfaceTintColor: Colors.transparent,
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
                const OnlineBadge(online: true),
              ],
            ),
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

// ─── Bottom nav bar ───────────────────────────────────────────────────────────

class _PosBottomNavBar extends StatelessWidget {
  const _PosBottomNavBar({required this.currentIndex});

  final int currentIndex;

  static const _destinations = [
    (icon: Icons.point_of_sale_rounded, label: 'Ventes', route: '/pos'),
    (icon: Icons.history_rounded, label: 'Historique', route: '/pos/history'),
    (icon: Icons.qr_code_scanner_rounded, label: 'Scanner', route: '/pos/scan'),
    (icon: Icons.person_rounded, label: 'Profil', route: '/pos/profile'),
  ];

  @override
  Widget build(BuildContext context) {
    return NavigationBar(
      selectedIndex: currentIndex,
      onDestinationSelected: (i) {
        if (i != currentIndex) context.go(_destinations[i].route);
      },
      destinations: [
        for (final d in _destinations)
          NavigationDestination(
            icon: Icon(d.icon),
            label: d.label,
          ),
      ],
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
