import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../design_system/layout/screen_size.dart';
import '../../../../design_system/tokens/tch_colors.dart';
import '../../../../design_system/tokens/tch_radius.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
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

// ─── Phone layout (< 960 px) ─────────────────────────────────────────────────

class _PhoneLayout extends StatelessWidget {
  const _PhoneLayout({
    required this.isLoading,
    required this.onLogin,
    this.errorMessage,
  });

  final bool isLoading;
  final String? errorMessage;
  final VoidCallback onLogin;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: TchSpacing.s24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Spacer(flex: 3),
          const _BrandBlock(iconSize: 80, large: false),
          const Spacer(flex: 2),
          if (errorMessage != null) ...[
            _ErrorBanner(message: errorMessage!),
            const SizedBox(height: TchSpacing.s16),
          ],
          _ConnectButton(isLoading: isLoading, height: 56, onPressed: onLogin),
          const Spacer(flex: 1),
        ],
      ),
    );
  }
}

// ─── POS terminal layout (≥ 960 px) ──────────────────────────────────────────

class _TerminalLayout extends StatelessWidget {
  const _TerminalLayout({
    required this.isLoading,
    required this.onLogin,
    this.errorMessage,
  });

  final bool isLoading;
  final String? errorMessage;
  final VoidCallback onLogin;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SizedBox(
        width: 420,
        child: Card(
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(TchRadius.xl),
            side: BorderSide(
              color: Theme.of(context).colorScheme.outlineVariant,
            ),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: TchSpacing.s40,
              vertical: TchSpacing.s48,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const _BrandBlock(iconSize: 96, large: true),
                const SizedBox(height: TchSpacing.s40),
                if (errorMessage != null) ...[
                  _ErrorBanner(message: errorMessage!),
                  const SizedBox(height: TchSpacing.s20),
                ],
                _ConnectButton(isLoading: isLoading, height: 64, onPressed: onLogin),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ─── Shared components ────────────────────────────────────────────────────────

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
