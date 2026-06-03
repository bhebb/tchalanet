import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../auth/data/models/user_session.dart';
import '../../../auth/presentation/view_models/auth_controller.dart';

class PosDashboardPage extends ConsumerWidget {
  const PosDashboardPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(userSessionProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Tchalanet POS'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Déconnexion',
            onPressed: () async {
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _SessionCard(session: session),
          ],
        ),
      ),
    );
  }
}

class _SessionCard extends StatelessWidget {
  const _SessionCard({required this.session});

  final UserSession session;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Session', style: textTheme.titleMedium),
            const Divider(),
            _Row(label: 'Utilisateur', value: session.username ?? '—'),
            _Row(
              label: 'Nom',
              value: session.displayName ?? session.username ?? '—',
            ),
            _Row(label: 'Tenant', value: session.tenantCode ?? session.tenantId ?? '—'),
            const SizedBox(height: 12),
            Text('Rôles', style: textTheme.titleSmall),
            const SizedBox(height: 8),
            if (session.roles.isEmpty)
              Text('Aucun rôle détecté', style: textTheme.bodySmall)
            else
              Wrap(
                spacing: 8,
                children: session.roles
                    .map(
                      (r) => Chip(
                        label: Text(r.name),
                        backgroundColor: colorScheme.secondaryContainer,
                        labelStyle:
                            TextStyle(color: colorScheme.onSecondaryContainer),
                      ),
                    )
                    .toList(),
              ),
            if (session.tokenExpiresAt != null) ...[
              const SizedBox(height: 12),
              _Row(
                label: 'Expiration token',
                value: session.tokenExpiresAt!.toLocal().toString(),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _Row extends StatelessWidget {
  const _Row({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 120,
            child: Text(
              label,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: Theme.of(context).colorScheme.outline,
                  ),
            ),
          ),
          Expanded(
            child: Text(value, style: Theme.of(context).textTheme.bodyMedium),
          ),
        ],
      ),
    );
  }
}
