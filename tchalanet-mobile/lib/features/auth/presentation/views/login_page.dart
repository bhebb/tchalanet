import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../design_system/layout/screen_size.dart';
import '../../../../design_system/tokens/tch_colors.dart';
import '../../../../design_system/tokens/tch_radius.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
import '../../../draw/data/models/draw_models.dart';
import '../../../draw/presentation/view_models/draw_providers.dart';
import '../view_models/auth_controller.dart';

class LoginPage extends ConsumerWidget {
  const LoginPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authControllerProvider);

    ref.listen<AuthState>(authControllerProvider, (_, next) {
      if (next is AuthAuthenticated) context.go('/pos');
    });

    final isLoading = authState is AuthLoading;
    final errorMessage =
        authState is AuthUnauthenticated ? authState.errorMessage : null;

    void onLogin() => ref.read(authControllerProvider.notifier).login();

    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surface,
      body: SafeArea(
        child: context.isPosTerminal
            ? _TerminalLayout(
                isLoading: isLoading,
                errorMessage: errorMessage,
                onLogin: onLogin,
              )
            : _PhoneLayout(
                isLoading: isLoading,
                errorMessage: errorMessage,
                onLogin: onLogin,
              ),
      ),
    );
  }
}

// ─── Phone layout ─────────────────────────────────────────────────────────────

class _PhoneLayout extends ConsumerWidget {
  const _PhoneLayout({
    required this.isLoading,
    required this.onLogin,
    this.errorMessage,
  });

  final bool isLoading;
  final String? errorMessage;
  final VoidCallback onLogin;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final slotsAsync = ref.watch(homeDrawSlotsProvider);
    final nextDraw = ref.watch(nextDrawProvider);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _CompactHeader(),
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: TchSpacing.s16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: TchSpacing.s16),
                if (nextDraw != null) ...[
                  _NextDrawCard(slot: nextDraw),
                  const SizedBox(height: TchSpacing.s12),
                ],
                slotsAsync.when(
                  data: (slots) => slots.isEmpty
                      ? const SizedBox.shrink()
                      : _DrawResultsSection(slots: slots),
                  loading: () => const _DrawLoadingShimmer(),
                  error: (_, _) => const SizedBox.shrink(),
                ),
                const SizedBox(height: TchSpacing.s16),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(
            TchSpacing.s16, TchSpacing.s8, TchSpacing.s16, TchSpacing.s16,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (errorMessage != null) ...[
                _ErrorBanner(message: errorMessage!),
                const SizedBox(height: TchSpacing.s8),
              ],
              _ConnectButton(isLoading: isLoading, height: 56, onPressed: onLogin),
            ],
          ),
        ),
      ],
    );
  }
}

// ─── POS terminal layout ──────────────────────────────────────────────────────

class _TerminalLayout extends ConsumerWidget {
  const _TerminalLayout({
    required this.isLoading,
    required this.onLogin,
    this.errorMessage,
  });

  final bool isLoading;
  final String? errorMessage;
  final VoidCallback onLogin;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final slotsAsync = ref.watch(homeDrawSlotsProvider);
    final nextDraw = ref.watch(nextDrawProvider);

    return Row(
      children: [
        // Left column — brand + connect
        SizedBox(
          width: 380,
          child: Padding(
            padding: const EdgeInsets.all(TchSpacing.s48),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const _BrandBlock(iconSize: 96, large: true),
                const SizedBox(height: TchSpacing.s40),
                if (errorMessage != null) ...[
                  _ErrorBanner(message: errorMessage!),
                  const SizedBox(height: TchSpacing.s16),
                ],
                _ConnectButton(isLoading: isLoading, height: 64, onPressed: onLogin),
              ],
            ),
          ),
        ),
        // Divider
        VerticalDivider(
          width: 1,
          color: Theme.of(context).colorScheme.outlineVariant,
        ),
        // Right column — draws
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(TchSpacing.s32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (nextDraw != null) ...[
                  _NextDrawCard(slot: nextDraw),
                  const SizedBox(height: TchSpacing.s16),
                ],
                slotsAsync.when(
                  data: (slots) => slots.isEmpty
                      ? const SizedBox.shrink()
                      : _DrawResultsSection(slots: slots),
                  loading: () => const _DrawLoadingShimmer(),
                  error: (_, _) => const SizedBox.shrink(),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

// ─── Shared components ────────────────────────────────────────────────────────

class _CompactHeader extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s16,
        vertical: TchSpacing.s12,
      ),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).colorScheme.outlineVariant,
          ),
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.primaryContainer,
              borderRadius: BorderRadius.circular(TchRadius.sm),
            ),
            child: Icon(
              Icons.point_of_sale_rounded,
              size: 20,
              color: Theme.of(context).colorScheme.primary,
            ),
          ),
          const SizedBox(width: TchSpacing.s12),
          Text(
            'Tchalanet POS',
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}

class _BrandBlock extends StatelessWidget {
  const _BrandBlock({required this.iconSize, required this.large});

  final double iconSize;
  final bool large;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final containerSize = iconSize + 40;

    return Column(
      children: [
        Container(
          width: containerSize,
          height: containerSize,
          decoration: BoxDecoration(
            color: scheme.primaryContainer,
            borderRadius: BorderRadius.circular(TchRadius.xl),
          ),
          child: Icon(
            Icons.point_of_sale_rounded,
            size: iconSize / 2,
            color: scheme.primary,
          ),
        ),
        const SizedBox(height: TchSpacing.s24),
        Text(
          'Tchalanet POS',
          style: (large
                  ? Theme.of(context).textTheme.displaySmall
                  : Theme.of(context).textTheme.headlineLarge)
              ?.copyWith(
            fontWeight: FontWeight.bold,
            color: scheme.onSurface,
            letterSpacing: -0.5,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: TchSpacing.s8),
        Text(
          'Connectez-vous avec votre compte Tchalanet',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: scheme.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}

class _NextDrawCard extends StatelessWidget {
  const _NextDrawCard({required this.slot});

  final DrawSlotView slot;

  @override
  Widget build(BuildContext context) {
    final next = slot.next!;
    final scheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(TchSpacing.s16),
      decoration: BoxDecoration(
        color: scheme.primaryContainer,
        borderRadius: BorderRadius.circular(TchRadius.md),
      ),
      child: Row(
        children: [
          Icon(Icons.timer_outlined, color: scheme.primary, size: 20),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Prochain tirage',
                  style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: scheme.onPrimaryContainer,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.5,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  slot.label,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: scheme.onPrimaryContainer,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: TchSpacing.s12,
              vertical: TchSpacing.s4,
            ),
            decoration: BoxDecoration(
              color: scheme.primary,
              borderRadius: BorderRadius.circular(TchRadius.pill),
            ),
            child: Text(
              next.formattedCountdown,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: scheme.onPrimary,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DrawResultsSection extends StatelessWidget {
  const _DrawResultsSection({required this.slots});

  final List<DrawSlotView> slots;

  @override
  Widget build(BuildContext context) {
    final withResults = slots.where((s) => s.latest?.numbers?.hasNumbers == true).toList();
    if (withResults.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: TchSpacing.s8),
          child: Text(
            'Résultats du jour',
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.5,
            ),
          ),
        ),
        Container(
          decoration: BoxDecoration(
            border: Border.all(
              color: Theme.of(context).colorScheme.outlineVariant,
            ),
            borderRadius: BorderRadius.circular(TchRadius.md),
          ),
          child: Column(
            children: [
              for (int i = 0; i < withResults.length; i++) ...[
                if (i > 0)
                  Divider(
                    height: 1,
                    color: Theme.of(context).colorScheme.outlineVariant,
                  ),
                _DrawResultRow(slot: withResults[i]),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

class _DrawResultRow extends StatelessWidget {
  const _DrawResultRow({required this.slot});

  final DrawSlotView slot;

  @override
  Widget build(BuildContext context) {
    final numbers = slot.latest!.numbers!.nonEmpty;
    final scheme = Theme.of(context).colorScheme;

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s16,
        vertical: TchSpacing.s12,
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  slot.label,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
                ),
                if (slot.displayDrawTime.isNotEmpty)
                  Text(
                    slot.displayDrawTime,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: scheme.outline,
                    ),
                  ),
              ],
            ),
          ),
          Wrap(
            spacing: TchSpacing.s8,
            children: numbers
                .map((n) => _NumberBadge(number: n))
                .toList(),
          ),
        ],
      ),
    );
  }
}

class _NumberBadge extends StatelessWidget {
  const _NumberBadge({required this.number});

  final String number;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 40,
      height: 40,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.secondaryContainer,
        borderRadius: BorderRadius.circular(TchRadius.sm),
      ),
      child: Text(
        number,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          fontWeight: FontWeight.bold,
          color: Theme.of(context).colorScheme.onSecondaryContainer,
        ),
      ),
    );
  }
}

class _DrawLoadingShimmer extends StatelessWidget {
  const _DrawLoadingShimmer();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        2,
        (_) => Padding(
          padding: const EdgeInsets.only(bottom: TchSpacing.s8),
          child: Container(
            height: 56,
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainer,
              borderRadius: BorderRadius.circular(TchRadius.md),
            ),
          ),
        ),
      ),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s16,
        vertical: TchSpacing.s12,
      ),
      decoration: BoxDecoration(
        color: TchColors.errorContainer,
        borderRadius: BorderRadius.circular(TchRadius.md),
        border: Border.all(color: TchColors.error.withValues(alpha: 0.2)),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline_rounded, color: TchColors.error, size: 20),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Text(
              message,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: TchColors.error,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ConnectButton extends StatelessWidget {
  const _ConnectButton({
    required this.isLoading,
    required this.height,
    required this.onPressed,
  });

  final bool isLoading;
  final double height;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: FilledButton(
        onPressed: isLoading ? null : onPressed,
        style: FilledButton.styleFrom(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(TchRadius.md),
          ),
        ),
        child: isLoading
            ? const SizedBox(
                height: 22,
                width: 22,
                child: CircularProgressIndicator(
                  strokeWidth: 2.5,
                  color: Colors.white,
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.login_rounded, size: 20),
                  const SizedBox(width: TchSpacing.s8),
                  Text(
                    'Se connecter',
                    style: Theme.of(context).textTheme.labelLarge?.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
