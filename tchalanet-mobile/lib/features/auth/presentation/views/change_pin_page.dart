import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/i18n/i18n_repository.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
import '../../../cashier/home/presentation/view_models/cashier_home_providers.dart';
import '../view_models/auth_controller.dart';
import '../view_models/change_pin_controller.dart';

class ChangePinPage extends ConsumerStatefulWidget {
  const ChangePinPage({super.key});

  @override
  ConsumerState<ChangePinPage> createState() => _ChangePinPageState();
}

class _ChangePinPageState extends ConsumerState<ChangePinPage> {
  final _formKey = GlobalKey<FormState>();
  final _pin = TextEditingController();
  final _confirmation = TextEditingController();
  var _completionHandled = false;

  @override
  void initState() {
    super.initState();
    // The confirmation field's validator compares against `_pin.text`, so it
    // must re-validate whenever the PIN changes — not just when the
    // confirmation field itself is edited — otherwise a stale mismatch can
    // linger on screen after the user fixes the first field.
    _pin.addListener(_revalidateIfConfirmationStarted);
  }

  @override
  void dispose() {
    _pin.removeListener(_revalidateIfConfirmationStarted);
    _pin.dispose();
    _confirmation.dispose();
    super.dispose();
  }

  void _revalidateIfConfirmationStarted() {
    if (_confirmation.text.isEmpty) return;
    _formKey.currentState?.validate();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    await ref.read(changePinControllerProvider.notifier).submit(_pin.text);
  }

  Future<void> _completePinChange() async {
    try {
      final home = await ref.refresh(cashierHomeProvider.future);
      if (!mounted) return;

      if (home.mustChangePin) {
        setState(() => _completionHandled = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              ref
                  .read(i18nBundleProvider)
                  .translate('auth.change_pin.refresh_failed'),
            ),
          ),
        );
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ref.read(i18nBundleProvider).translate('auth.change_pin.success'),
          ),
        ),
      );
      ref.invalidate(cashierReadinessProvider);
      ref.invalidate(terminalDailyStatsProvider);
      ref.invalidate(availableDrawsProvider);
      context.go('/pos');
    } catch (_) {
      if (!mounted) return;
      setState(() => _completionHandled = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            ref
                .read(i18nBundleProvider)
                .translate('auth.change_pin.refresh_failed'),
          ),
        ),
      );
    }
  }

  Future<void> _confirmLogout() async {
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
    if (mounted) context.go('/login');
  }

  String _errorTranslationKey(List<String> errorKeys) {
    final translations = ref.read(i18nBundleProvider);
    for (final key in errorKeys) {
      if (translations.translate(key) != key) return key;
    }
    return 'common.error.unknown';
  }

  @override
  Widget build(BuildContext context) {
    final translations = ref.watch(i18nBundleProvider);
    final state = ref.watch(changePinControllerProvider);

    ref.listen<ChangePinState>(changePinControllerProvider, (_, next) {
      if (!next.completed || _completionHandled) return;
      _completionHandled = true;
      _completePinChange();
    });

    final errorKey = state.errorKeys.isEmpty
        ? null
        : _errorTranslationKey(state.errorKeys);
    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('auth.change_pin.title')),
        // This screen can be reached with nothing left to pop back to (the
        // forced-PIN-change redirect replaces the route) — always offer a
        // way out via sign-out rather than trapping the seller here.
        actions: [
          TextButton(
            onPressed: state.submitting ? null : () => _confirmLogout(),
            child: Text(translations.translate('pos.profile.sign_out')),
          ),
        ],
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          autovalidateMode: AutovalidateMode.onUserInteraction,
          child: ListView(
            padding: const EdgeInsets.all(TchSpacing.s20),
            children: [
              Text(translations.translate('auth.change_pin.description')),
              const SizedBox(height: TchSpacing.s24),
              TextFormField(
                controller: _pin,
                autofocus: true,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 6,
                decoration: InputDecoration(
                  labelText: translations.translate('auth.change_pin.new_pin'),
                  prefixIcon: const Icon(Icons.pin_outlined),
                ),
                validator: (value) =>
                    value != null && RegExp(r'^\d{6}$').hasMatch(value)
                    ? null
                    : translations.translate('auth.change_pin.invalid'),
              ),
              const SizedBox(height: TchSpacing.s12),
              TextFormField(
                controller: _confirmation,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 6,
                decoration: InputDecoration(
                  labelText: translations.translate(
                    'auth.change_pin.confirm_pin',
                  ),
                  prefixIcon: const Icon(Icons.lock_outline_rounded),
                ),
                validator: (value) => value == _pin.text
                    ? null
                    : translations.translate('auth.change_pin.mismatch'),
              ),
              if (errorKey != null) ...[
                const SizedBox(height: TchSpacing.s12),
                Text(
                  translations.translate(errorKey),
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ],
              const SizedBox(height: TchSpacing.s24),
              FilledButton(
                onPressed: state.submitting
                    ? null
                    : state.completed
                    ? (_completionHandled ? null : _completePinChange)
                    : _submit,
                child: Text(
                  translations.translate(
                    state.submitting
                        ? 'common.saving'
                        : state.completed
                        ? 'common.retry'
                        : 'auth.change_pin.submit',
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
