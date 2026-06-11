import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/i18n/i18n_repository.dart';
import '../../../../design_system/components/components.dart';
import '../view_models/auth_controller.dart';
import '../view_models/forbidden_view_model.dart';

class ForbiddenPage extends ConsumerWidget {
  const ForbiddenPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(forbiddenViewModelProvider);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: FeedbackState(
                kind: FeedbackStateKind.blocked,
                title: translations.translate(state.titleKey),
                message: translations.translate(state.messageKey),
                actionLabel: translations.translate(state.backActionKey),
                onAction: () async {
                  await ref.read(authControllerProvider.notifier).logout();
                  if (context.mounted) context.go('/login');
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}
