import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../core/runtime/runtime_controller.dart';
import '../../../../../design_system/components/feedback_state.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../view_models/cashier_home_providers.dart';

class SellerTerminalProfilePage extends ConsumerWidget {
  const SellerTerminalProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homeAsync = ref.watch(cashierHomeProvider);
    final runtimeState = ref.watch(runtimeControllerProvider);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(title: Text(translations.translate('pos.profile.title'))),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(cashierHomeProvider);
            ref.invalidate(runtimeControllerProvider);
          },
          child: homeAsync.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (error, _) => ListView(
              padding: const EdgeInsets.all(TchSpacing.s24),
              children: [
                FeedbackState(
                  kind: FeedbackStateKind.offline,
                  title: translations.translate('pos.profile.title'),
                  message: translations.translate('common.error.network'),
                ),
              ],
            ),
            data: (home) => ListView(
              padding: const EdgeInsets.all(TchSpacing.s16),
              children: [
                _ProfileHeader(
                  label:
                      home.operationalContext?.sellerTerminalLabel ??
                      home.header?.title ??
                      translations.translate('pos.profile.terminal'),
                  subtitle: home.header?.subtitle,
                  ready: home.operationalContext?.ready ?? home.isOperational,
                ),
                const SizedBox(height: TchSpacing.s16),
                _Section(
                  title: translations.translate('pos.profile.terminal'),
                  children: [
                    _InfoRow(
                      icon: Icons.badge_rounded,
                      label: translations.translate('pos.profile.name'),
                      value:
                          home.operationalContext?.sellerTerminalLabel ?? '-',
                    ),
                    _InfoRow(
                      icon: Icons.verified_user_rounded,
                      label: translations.translate('pos.profile.context'),
                      value: home.operationalContext?.trusted == true
                          ? translations.translate('pos.profile.verified')
                          : translations.translate(
                              'pos.profile.needs_verification',
                            ),
                    ),
                    _InfoRow(
                      icon: Icons.confirmation_number_rounded,
                      label: translations.translate(
                        'pos.profile.tickets_today',
                      ),
                      value: '${home.session?.ticketCount ?? 0}',
                    ),
                    _InfoRow(
                      icon: Icons.payments_rounded,
                      label: translations.translate('pos.profile.sales_today'),
                      value:
                          home.session?.salesTotal ??
                          '0 ${home.currency ?? 'HTG'}',
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s16),
                _Section(
                  title: translations.translate('pos.profile.account'),
                  children: [
                    _InfoRow(
                      icon: Icons.person_rounded,
                      label: translations.translate('pos.profile.user'),
                      value:
                          runtimeState.bootstrap?.user?.displayName ??
                          runtimeState.bootstrap?.user?.username ??
                          '-',
                    ),
                    _InfoRow(
                      icon: Icons.store_rounded,
                      label: translations.translate('pos.profile.tenant'),
                      value:
                          runtimeState.bootstrap?.tenantContext?.tenantCode ??
                          runtimeState.bootstrap?.tenantContext?.tenantId ??
                          '-',
                    ),
                    _InfoRow(
                      icon: Icons.health_and_safety_rounded,
                      label: translations.translate('pos.profile.readiness'),
                      value: runtimeState.snapshot?.readinessStatus ?? 'READY',
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s24),
                _Section(
                  title: translations.translate('pos.profile.app'),
                  children: [
                    _LocaleSelector(
                      label: translations.translate('pos.profile.language'),
                    ),
                    _InfoRow(
                      icon: Icons.info_outline_rounded,
                      label: translations.translate('pos.profile.version'),
                      value: '1.0.0',
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s24),
                FilledButton.icon(
                  onPressed: () async {
                    await context.push('/change-pin');
                    ref.invalidate(cashierHomeProvider);
                  },
                  icon: const Icon(Icons.password_rounded),
                  label: Text(translations.translate('pos.profile.change_pin')),
                ),
                const SizedBox(height: TchSpacing.s12),
                FilledButton.icon(
                  onPressed: () {
                    ref.invalidate(cashierHomeProvider);
                    ref.invalidate(runtimeControllerProvider);
                  },
                  icon: const Icon(Icons.refresh_rounded),
                  label: Text(translations.translate('pos.profile.refresh')),
                ),
                const SizedBox(height: TchSpacing.s12),
                OutlinedButton.icon(
                  onPressed: () => _confirmLogout(context, ref, translations),
                  icon: const Icon(Icons.logout_rounded),
                  label: Text(translations.translate('pos.profile.sign_out')),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

Future<void> _confirmLogout(
  BuildContext context,
  WidgetRef ref,
  I18nBundle translations,
) async {
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => AlertDialog(
      title: Text(translations.translate('pos.profile.sign_out_title')),
      content: Text(translations.translate('pos.profile.sign_out_message')),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(dialogContext).pop(false),
          child: Text(translations.translate('common.cancel')),
        ),
        FilledButton(
          onPressed: () => Navigator.of(dialogContext).pop(true),
          child: Text(translations.translate('pos.profile.sign_out')),
        ),
      ],
    ),
  );
  if (confirmed != true) return;

  await ref.read(authControllerProvider.notifier).logout();
  if (context.mounted) context.go('/login');
}

class _LocaleSelector extends ConsumerWidget {
  const _LocaleSelector({required this.label});

  final String label;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final activeLocale = ref.watch(localeProvider);
    return ListTile(
      leading: const Icon(Icons.language_rounded),
      title: Text(label),
      subtitle: SegmentedButton<String>(
        showSelectedIcon: false,
        segments: const [
          ButtonSegment(value: 'ht', label: Text('Kreyòl')),
          ButtonSegment(value: 'fr', label: Text('Français')),
          ButtonSegment(value: 'en', label: Text('English')),
        ],
        selected: {activeLocale},
        onSelectionChanged: (selection) =>
            ref.read(localeProvider.notifier).setLocale(selection.single),
      ),
    );
  }
}

class _ProfileHeader extends StatelessWidget {
  const _ProfileHeader({
    required this.label,
    required this.ready,
    this.subtitle,
  });

  final String label;
  final String? subtitle;
  final bool ready;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(TchRadius.lg),
      ),
      child: Padding(
        padding: const EdgeInsets.all(TchSpacing.s20),
        child: Row(
          children: [
            CircleAvatar(
              radius: 28,
              backgroundColor: ready ? scheme.primary : scheme.errorContainer,
              foregroundColor: ready
                  ? scheme.onPrimary
                  : scheme.onErrorContainer,
              child: const Icon(Icons.point_of_sale_rounded),
            ),
            const SizedBox(width: TchSpacing.s16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  if (subtitle != null && subtitle!.isNotEmpty) ...[
                    const SizedBox(height: TchSpacing.s4),
                    Text(
                      subtitle!,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
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

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: TchSpacing.s4),
          child: Text(
            title,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: Theme.of(context).colorScheme.onSurfaceVariant,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        const SizedBox(height: TchSpacing.s8),
        DecoratedBox(
          decoration: BoxDecoration(
            border: Border.all(
              color: Theme.of(context).colorScheme.outlineVariant,
            ),
            borderRadius: BorderRadius.circular(TchRadius.md),
          ),
          child: Column(children: children),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListTile(
      leading: Icon(icon, color: scheme.primary),
      title: Text(label),
      subtitle: Text(value, maxLines: 2, overflow: TextOverflow.ellipsis),
    );
  }
}
