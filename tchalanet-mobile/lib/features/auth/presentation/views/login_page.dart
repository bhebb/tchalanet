import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/auth/auth_token_client.dart';
import '../../../../core/config/app_config.dart';
import '../../../../core/i18n/i18n_repository.dart';
import '../../../../design_system/components/components.dart';
import '../../../../design_system/layout/screen_size.dart';
import '../../../../design_system/tokens/tch_radius.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
import '../view_models/auth_controller.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _terminalCodeController = TextEditingController();
  final _pinController = TextEditingController();

  @override
  void dispose() {
    _terminalCodeController.dispose();
    _pinController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    ref.read(authControllerProvider.notifier).login(
      AuthCredentials.terminal(
        terminalCode: _terminalCodeController.text.trim(),
        pin: _pinController.text,
        domain: terminalEmailDomain,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authControllerProvider);
    final translations = ref.watch(i18nBundleProvider);
    final isLoading = authState is AuthLoading;
    final errorMessage =
        authState is AuthUnauthenticated ? authState.errorKey : null;

    final form = _TerminalLoginForm(
      formKey: _formKey,
      terminalCodeController: _terminalCodeController,
      pinController: _pinController,
      loading: isLoading,
      errorMessage:
          errorMessage == null ? null : translations.translate(errorMessage),
      terminalCodeLabel: translations.translate('auth.login.terminal_code'),
      terminalCodeHint: translations.translate('auth.login.terminal_code_hint'),
      pinLabel: translations.translate('auth.login.pin'),
      submitLabel: translations.translate('auth.login.button'),
      blockedLabel: translations.translate('auth.login.blocked'),
      requiredLabel: translations.translate('auth.login.required'),
      onSubmit: _submit,
    );

    return Scaffold(
      body: SafeArea(
        child: context.isPosTerminal
            ? _PosLoginLayout(form: form)
            : _MobileLoginLayout(form: form),
      ),
    );
  }
}

class _MobileLoginLayout extends ConsumerWidget {
  const _MobileLoginLayout({required this.form});

  final Widget form;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(TchSpacing.s16),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 480),
          child: Column(
            children: [
              _BrandHeader(
                title: translations.translate('auth.login.terminal_title'),
                subtitle: translations.translate('auth.login.subtitle'),
                icon: Icons.point_of_sale_rounded,
              ),
              const SizedBox(height: TchSpacing.s32),
              SurfaceCard(
                padding: const EdgeInsets.all(TchSpacing.s24),
                child: form,
              ),
              const SizedBox(height: TchSpacing.s24),
              _SecurityFooter(
                label: translations.translate('auth.login.secure_environment'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PosLoginLayout extends ConsumerWidget {
  const _PosLoginLayout({required this.form});

  final Widget form;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Expanded(
          child: ColoredBox(
            color: scheme.primaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(TchSpacing.s48),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _BrandHeader(
                    title: translations.translate('auth.login.terminal_title'),
                    subtitle: translations.translate('auth.login.pos_subtitle'),
                    icon: Icons.point_of_sale_rounded,
                    inverse: true,
                  ),
                  const SizedBox(height: TchSpacing.s32),
                  _PosSecurityNotice(
                    title: translations.translate('auth.login.terminal_mode'),
                    message: translations.translate(
                      'auth.login.terminal_binding_notice',
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        Expanded(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(TchSpacing.s48),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 520),
                child: SurfaceCard(
                  padding: const EdgeInsets.all(TchSpacing.s32),
                  child: form,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _BrandHeader extends StatelessWidget {
  const _BrandHeader({
    required this.title,
    required this.subtitle,
    required this.icon,
    this.inverse = false,
  });

  final String title;
  final String subtitle;
  final IconData icon;
  final bool inverse;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final foreground = inverse ? scheme.onPrimaryContainer : scheme.onSurface;
    final secondary = inverse
        ? scheme.onPrimaryContainer.withValues(alpha: 0.78)
        : scheme.onSurfaceVariant;
    return Column(
      crossAxisAlignment:
          inverse ? CrossAxisAlignment.start : CrossAxisAlignment.center,
      children: [
        Container(
          width: 88,
          height: 88,
          decoration: BoxDecoration(
            color: inverse ? scheme.tertiary : scheme.primaryContainer,
            borderRadius: BorderRadius.circular(TchRadius.xl),
          ),
          child: Icon(
            icon,
            size: 44,
            color: inverse ? scheme.onTertiary : scheme.onPrimaryContainer,
          ),
        ),
        const SizedBox(height: TchSpacing.s24),
        Text(
          title,
          textAlign: inverse ? TextAlign.start : TextAlign.center,
          style: Theme.of(context).textTheme.headlineMedium?.copyWith(
            color: foreground,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: TchSpacing.s8),
        Text(
          subtitle,
          textAlign: inverse ? TextAlign.start : TextAlign.center,
          style: Theme.of(
            context,
          ).textTheme.bodyLarge?.copyWith(color: secondary),
        ),
      ],
    );
  }
}

class _TerminalLoginForm extends StatelessWidget {
  const _TerminalLoginForm({
    required this.formKey,
    required this.terminalCodeController,
    required this.pinController,
    required this.loading,
    required this.terminalCodeLabel,
    required this.terminalCodeHint,
    required this.pinLabel,
    required this.submitLabel,
    required this.blockedLabel,
    required this.requiredLabel,
    required this.onSubmit,
    this.errorMessage,
  });

  final GlobalKey<FormState> formKey;
  final TextEditingController terminalCodeController;
  final TextEditingController pinController;
  final bool loading;
  final String terminalCodeLabel;
  final String terminalCodeHint;
  final String pinLabel;
  final String submitLabel;
  final String blockedLabel;
  final String requiredLabel;
  final String? errorMessage;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Form(
      key: formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            submitLabel,
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: TchSpacing.s24),
          TextFormField(
            controller: terminalCodeController,
            enabled: !loading,
            keyboardType: TextInputType.text,
            textInputAction: TextInputAction.next,
            textCapitalization: TextCapitalization.characters,
            decoration: InputDecoration(
              labelText: terminalCodeLabel,
              hintText: terminalCodeHint,
              prefixIcon: const Icon(Icons.terminal_rounded),
            ),
            validator: (value) =>
                value == null || value.trim().isEmpty ? requiredLabel : null,
          ),
          const SizedBox(height: TchSpacing.s16),
          TextFormField(
            controller: pinController,
            enabled: !loading,
            obscureText: true,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.done,
            onFieldSubmitted: (_) => onSubmit(),
            decoration: InputDecoration(
              labelText: pinLabel,
              prefixIcon: const Icon(Icons.pin_outlined),
            ),
            validator: (value) =>
                value == null || value.isEmpty ? requiredLabel : null,
          ),
          if (errorMessage != null) ...[
            const SizedBox(height: TchSpacing.s16),
            FieldError(message: errorMessage!),
          ],
          const SizedBox(height: TchSpacing.s24),
          PrimaryActionButton(
            label: submitLabel,
            icon: Icons.login_rounded,
            loading: loading,
            onPressed: onSubmit,
          ),
          const SizedBox(height: TchSpacing.s8),
          TextButton(
            onPressed: loading ? null : () {},
            child: Text(blockedLabel),
          ),
        ],
      ),
    );
  }
}

class _PosSecurityNotice extends StatelessWidget {
  const _PosSecurityNotice({required this.title, required this.message});

  final String title;
  final String message;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(TchSpacing.s20),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLowest.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(TchRadius.lg),
        border: Border.all(
          color: scheme.onPrimaryContainer.withValues(alpha: 0.24),
        ),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.verified_user_outlined, color: scheme.tertiary),
          const SizedBox(width: TchSpacing.s12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    color: scheme.onPrimaryContainer,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: TchSpacing.s4),
                Text(
                  message,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: scheme.onPrimaryContainer.withValues(alpha: 0.78),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SecurityFooter extends StatelessWidget {
  const _SecurityFooter({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(
          Icons.verified_user_outlined,
          size: 18,
          color: Theme.of(context).colorScheme.primary,
        ),
        const SizedBox(width: TchSpacing.s8),
        Text(
          label,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}
