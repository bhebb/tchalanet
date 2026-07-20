import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/i18n/i18n_repository.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
import '../../../cashier/home/presentation/view_models/cashier_home_providers.dart';
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
  void dispose() {
    _pin.dispose();
    _confirmation.dispose();
    super.dispose();
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
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
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
