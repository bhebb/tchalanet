import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../data/models/op_context_options.dart';
import '../view_models/op_context_setup_controller.dart';

class CashierSetupPage extends ConsumerStatefulWidget {
  const CashierSetupPage({super.key});

  @override
  ConsumerState<CashierSetupPage> createState() => _CashierSetupPageState();
}

class _CashierSetupPageState extends ConsumerState<CashierSetupPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(
        () => ref.read(opContextSetupControllerProvider.notifier).loadOptions());
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(opContextSetupControllerProvider);

    ref.listen<OpContextSetupState>(opContextSetupControllerProvider,
        (_, next) {
      if (next is OpContextDoneState) context.pop();
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Configurer le poste'),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: switch (state) {
        OpContextLoadingState() => const Center(child: CircularProgressIndicator()),
        OpContextErrorState(:final message) => _ErrorView(
            message: message,
            onRetry: () =>
                ref.read(opContextSetupControllerProvider.notifier).loadOptions(),
          ),
        OpContextDoneState() => const Center(child: CircularProgressIndicator()),
        OpContextSelectingState(:final options, :final selectedOutletId) =>
          _PickerBody(
            options: options,
            selectedOutletId: selectedOutletId,
            isSelecting: true,
          ),
        OpContextLoadedState(:final options, :final selectedOutletId, :final error) =>
          _PickerBody(
            options: options,
            selectedOutletId: selectedOutletId,
            isSelecting: false,
            error: error,
          ),
      },
    );
  }
}

// ─── Main picker body ─────────────────────────────────────────────────────────

class _PickerBody extends ConsumerWidget {
  const _PickerBody({
    required this.options,
    required this.selectedOutletId,
    required this.isSelecting,
    this.error,
  });

  final OpContextOptionsView options;
  final String? selectedOutletId;
  final bool isSelecting;
  final String? error;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Auto-select: 1 outlet + 1 terminal — trigger immediately
    if (options.canAutoSelect && !isSelecting) {
      final outlet = options.outlets.first;
      final terminal = options.terminals.first;
      Future.microtask(() {
        ref.read(opContextSetupControllerProvider.notifier).confirmSelection(
              outletId: outlet.outletId,
              terminalId: terminal.terminalId,
            );
      });
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: TchSpacing.s16),
            Text('Configuration automatique…'),
          ],
        ),
      );
    }

    // Step 1: outlet picker (multiple outlets, none selected yet)
    if (options.outlets.length > 1 && selectedOutletId == null) {
      return _OutletPicker(outlets: options.outlets, isSelecting: isSelecting);
    }

    // Step 2: terminal picker
    final activeOutletId = selectedOutletId ?? options.outlets.firstOrNull?.outletId;
    if (activeOutletId == null) {
      return const Center(child: Text('Aucun point de vente disponible.'));
    }
    final terminals = options.terminalsForOutlet(activeOutletId);
    return _TerminalPicker(
      outletName: options.outlets
          .where((o) => o.outletId == activeOutletId)
          .firstOrNull
          ?.name,
      outletId: activeOutletId,
      terminals: terminals,
      isSelecting: isSelecting,
      error: error,
      onBack: options.outlets.length > 1
          ? () => ref
              .read(opContextSetupControllerProvider.notifier)
              .selectOutlet('')
          : null,
    );
  }
}

// ─── Outlet picker ────────────────────────────────────────────────────────────

class _OutletPicker extends ConsumerWidget {
  const _OutletPicker({required this.outlets, required this.isSelecting});

  final List<OutletOption> outlets;
  final bool isSelecting;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            TchSpacing.s16, TchSpacing.s16, TchSpacing.s16, TchSpacing.s8,
          ),
          child: Text(
            'Sélectionnez un point de vente',
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(
              horizontal: TchSpacing.s16,
              vertical: TchSpacing.s8,
            ),
            itemCount: outlets.length,
            separatorBuilder: (_, _) => const SizedBox(height: TchSpacing.s8),
            itemBuilder: (context, i) {
              final outlet = outlets[i];
              return _OptionCard(
                title: outlet.name,
                subtitle: outlet.kind,
                icon: Icons.store_rounded,
                enabled: !isSelecting,
                onTap: () => ref
                    .read(opContextSetupControllerProvider.notifier)
                    .selectOutlet(outlet.outletId),
              );
            },
          ),
        ),
      ],
    );
  }
}

// ─── Terminal picker ──────────────────────────────────────────────────────────

class _TerminalPicker extends ConsumerWidget {
  const _TerminalPicker({
    required this.outletId,
    required this.terminals,
    required this.isSelecting,
    this.outletName,
    this.error,
    this.onBack,
  });

  final String outletId;
  final String? outletName;
  final List<TerminalOption> terminals;
  final bool isSelecting;
  final String? error;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final textTheme = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (outletName != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TchSpacing.s16, TchSpacing.s16, TchSpacing.s16, 0,
            ),
            child: Row(
              children: [
                if (onBack != null) ...[
                  IconButton(
                    icon: const Icon(Icons.arrow_back_rounded),
                    onPressed: onBack,
                    visualDensity: VisualDensity.compact,
                  ),
                  const SizedBox(width: TchSpacing.s4),
                ],
                Expanded(
                  child: Text(
                    outletName!,
                    style: textTheme.titleSmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ),
        Padding(
          padding: const EdgeInsets.fromLTRB(
            TchSpacing.s16, TchSpacing.s8, TchSpacing.s16, TchSpacing.s8,
          ),
          child: Text(
            'Sélectionnez un terminal',
            style: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        if (error != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TchSpacing.s16, 0, TchSpacing.s16, TchSpacing.s8,
            ),
            child: Container(
              padding: const EdgeInsets.all(TchSpacing.s12),
              decoration: BoxDecoration(
                color: scheme.errorContainer,
                borderRadius: BorderRadius.circular(TchRadius.md),
              ),
              child: Text(
                error!,
                style: textTheme.bodySmall?.copyWith(color: scheme.onErrorContainer),
              ),
            ),
          ),
        if (terminals.isEmpty)
          Padding(
            padding: const EdgeInsets.all(TchSpacing.s24),
            child: Text(
              'Aucun terminal disponible pour ce point de vente.',
              style: textTheme.bodyMedium?.copyWith(
                color: scheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
          )
        else
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(
                horizontal: TchSpacing.s16,
                vertical: TchSpacing.s8,
              ),
              itemCount: terminals.length,
              separatorBuilder: (_, _) => const SizedBox(height: TchSpacing.s8),
              itemBuilder: (context, i) {
                final terminal = terminals[i];
                return _OptionCard(
                  title: terminal.displayLabel,
                  subtitle: _terminalSubtitle(terminal),
                  icon: _terminalIcon(terminal.kind),
                  enabled: !isSelecting && terminal.canSell,
                  badge: terminal.canSell ? null : 'Indisponible',
                  onTap: () => ref
                      .read(opContextSetupControllerProvider.notifier)
                      .confirmSelection(
                        outletId: outletId,
                        terminalId: terminal.terminalId,
                      ),
                );
              },
            ),
          ),
      ],
    );
  }

  String? _terminalSubtitle(TerminalOption t) {
    if (t.kind == null) return null;
    return switch (t.kind) {
      'PHYSICAL' => 'Terminal POS',
      'VIRTUAL' => 'Application mobile',
      _ => t.kind,
    };
  }

  IconData _terminalIcon(String? kind) => switch (kind) {
        'PHYSICAL' => Icons.point_of_sale_rounded,
        'VIRTUAL' => Icons.phone_android_rounded,
        _ => Icons.devices_rounded,
      };
}

// ─── Option card ──────────────────────────────────────────────────────────────

class _OptionCard extends StatelessWidget {
  const _OptionCard({
    required this.title,
    required this.icon,
    required this.onTap,
    required this.enabled,
    this.subtitle,
    this.badge,
  });

  final String title;
  final String? subtitle;
  final IconData icon;
  final bool enabled;
  final String? badge;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Material(
      color: enabled ? scheme.surface : scheme.surfaceContainerLow,
      borderRadius: BorderRadius.circular(TchRadius.md),
      child: InkWell(
        onTap: enabled ? onTap : null,
        borderRadius: BorderRadius.circular(TchRadius.md),
        child: Container(
          padding: const EdgeInsets.all(TchSpacing.s16),
          decoration: BoxDecoration(
            border: Border.all(
              color: enabled ? scheme.outlineVariant : scheme.outlineVariant.withValues(alpha: 0.5),
            ),
            borderRadius: BorderRadius.circular(TchRadius.md),
          ),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: enabled
                      ? scheme.primaryContainer
                      : scheme.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(TchRadius.sm),
                ),
                child: Icon(
                  icon,
                  size: 22,
                  color: enabled ? scheme.primary : scheme.outline,
                ),
              ),
              const SizedBox(width: TchSpacing.s16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: textTheme.bodyLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: enabled
                            ? scheme.onSurface
                            : scheme.onSurface.withValues(alpha: 0.5),
                      ),
                    ),
                    if (subtitle != null)
                      Text(
                        subtitle!,
                        style: textTheme.bodySmall?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                  ],
                ),
              ),
              if (badge != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: TchSpacing.s8,
                    vertical: TchSpacing.s4,
                  ),
                  decoration: BoxDecoration(
                    color: scheme.errorContainer,
                    borderRadius: BorderRadius.circular(TchRadius.pill),
                  ),
                  child: Text(
                    badge!,
                    style: textTheme.labelSmall?.copyWith(
                      color: scheme.onErrorContainer,
                    ),
                  ),
                )
              else if (enabled)
                Icon(Icons.chevron_right_rounded, color: scheme.outline),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── Error view ───────────────────────────────────────────────────────────────

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(TchSpacing.s24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.cloud_off_rounded, size: 48, color: scheme.error),
            const SizedBox(height: TchSpacing.s16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
            const SizedBox(height: TchSpacing.s24),
            FilledButton.tonal(
              onPressed: onRetry,
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }
}
