import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/runtime/runtime_controller.dart';
import '../../../../../design_system/components/feedback_state.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../view_models/cashier_home_providers.dart';
import 'seller_terminal_nav_bar.dart';

class SellerTerminalProfilePage extends ConsumerWidget {
  const SellerTerminalProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final homeAsync = ref.watch(cashierHomeProvider);
    final runtimeState = ref.watch(runtimeControllerProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Profil')),
      bottomNavigationBar: const SellerTerminalNavBar(currentIndex: 3),
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
              children: const [
                FeedbackState(
                  kind: FeedbackStateKind.offline,
                  title: 'Profil indisponible',
                  message: 'Verifiez la connexion puis reessayez.',
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
                      'Terminal vendeur',
                  subtitle: home.header?.subtitle,
                  ready: home.operationalContext?.ready ?? home.isOperational,
                ),
                const SizedBox(height: TchSpacing.s16),
                _Section(
                  title: 'Terminal',
                  children: [
                    _InfoRow(
                      icon: Icons.badge_rounded,
                      label: 'Nom',
                      value:
                          home.operationalContext?.sellerTerminalLabel ??
                          'Non renseigne',
                    ),
                    _InfoRow(
                      icon: Icons.verified_user_rounded,
                      label: 'Contexte',
                      value: home.operationalContext?.trusted == true
                          ? 'Verifie'
                          : 'A verifier',
                    ),
                    _InfoRow(
                      icon: Icons.confirmation_number_rounded,
                      label: 'Tickets aujourd hui',
                      value: '${home.session?.ticketCount ?? 0}',
                    ),
                    _InfoRow(
                      icon: Icons.payments_rounded,
                      label: 'Ventes aujourd hui',
                      value:
                          home.session?.salesTotal ??
                          '0 ${home.currency ?? 'HTG'}',
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s16),
                _Section(
                  title: 'Compte',
                  children: [
                    _InfoRow(
                      icon: Icons.person_rounded,
                      label: 'Utilisateur',
                      value:
                          runtimeState.bootstrap?.user?.displayName ??
                          runtimeState.bootstrap?.user?.username ??
                          'Connecte',
                    ),
                    _InfoRow(
                      icon: Icons.store_rounded,
                      label: 'Tenant',
                      value:
                          runtimeState.bootstrap?.tenantContext?.tenantCode ??
                          runtimeState.bootstrap?.tenantContext?.tenantId ??
                          'Actif',
                    ),
                    _InfoRow(
                      icon: Icons.health_and_safety_rounded,
                      label: 'Readiness',
                      value: runtimeState.snapshot?.readinessStatus ?? 'READY',
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s24),
                FilledButton.icon(
                  onPressed: () {
                    ref.invalidate(cashierHomeProvider);
                    ref.invalidate(runtimeControllerProvider);
                  },
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Actualiser'),
                ),
              ],
            ),
          ),
        ),
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
