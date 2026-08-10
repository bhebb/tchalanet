import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/i18n/i18n_repository.dart';
import '../../features/auth/presentation/view_models/auth_controller.dart';

Future<void> showLogoutConfirmation(BuildContext context, WidgetRef ref) async {
  final translations = ref.read(i18nBundleProvider);
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
