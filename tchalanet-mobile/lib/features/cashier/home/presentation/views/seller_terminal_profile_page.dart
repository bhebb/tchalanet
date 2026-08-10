import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../design_system/components/components.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../../data/models/pos_profile_models.dart';
import '../view_models/cashier_home_providers.dart';

class SellerTerminalProfilePage extends ConsumerWidget {
  const SellerTerminalProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profileAsync = ref.watch(posProfileProvider);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(title: Text(translations.translate('pos.profile.title'))),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(posProfileProvider);
          },
          child: profileAsync.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (_, _) => const _ProfileUnavailable(),
            data: (profile) => ListView(
              padding: const EdgeInsets.all(TchSpacing.s16),
              children: [
                _ProfileHeader(
                  label: _valueOrDash(
                    profile.terminal.displayName,
                    translations.translate('pos.profile.terminal'),
                  ),
                  subtitle: profile.terminal.code,
                  ready: profile.terminal.ready,
                ),
                const SizedBox(height: TchSpacing.s16),
                _Section(
                  title: translations.translate('pos.profile.terminal'),
                  children: [
                    _InfoRow(
                      icon: Icons.badge_rounded,
                      label: translations.translate('pos.profile.name'),
                      value: _valueOrDash(profile.terminal.displayName),
                    ),
                    _InfoRow(
                      icon: Icons.confirmation_number_rounded,
                      label: translations.translate('pos.profile.code'),
                      value: _valueOrDash(profile.terminal.code),
                    ),
                    _InfoRow(
                      icon: Icons.health_and_safety_rounded,
                      label: translations.translate('pos.profile.status'),
                      value: _terminalStatusLabel(translations, profile),
                    ),
                    _InfoRow(
                      icon: Icons.verified_user_rounded,
                      label: translations.translate('pos.profile.context'),
                      value: profile.terminal.trusted
                          ? translations.translate('pos.profile.verified')
                          : translations.translate(
                              'pos.profile.needs_verification',
                            ),
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
                      value: _valueOrDash(profile.seller.displayName),
                    ),
                    _InfoRow(
                      icon: Icons.mail_rounded,
                      label: translations.translate('pos.profile.email'),
                      value: _valueOrDash(profile.seller.email),
                    ),
                    _InfoRow(
                      icon: Icons.phone_rounded,
                      label: translations.translate('pos.profile.phone'),
                      value: _valueOrDash(profile.seller.phoneNumber),
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s24),
                _Section(
                  title: translations.translate('pos.profile.commercial'),
                  children: [
                    _InfoRow(
                      icon: Icons.store_rounded,
                      label: translations.translate('pos.profile.tenant'),
                      value: _valueOrDash(
                        profile.commercial.tenantCode ??
                            profile.commercial.tenantId,
                      ),
                    ),
                    _InfoRow(
                      icon: Icons.payments_rounded,
                      label: translations.translate('pos.profile.currency'),
                      value: _valueOrDash(profile.commercial.currency),
                    ),
                    _InfoRow(
                      icon: Icons.percent_rounded,
                      label: translations.translate('pos.profile.commission'),
                      value: _commissionLabel(profile.commercial),
                    ),
                  ],
                ),
                const SizedBox(height: TchSpacing.s24),
                FilledButton.icon(
                  onPressed: () => context.go('/pos/settings'),
                  icon: const Icon(Icons.settings_rounded),
                  label: Text(translations.translate('pos.settings.title')),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// The profile remains an escape hatch when operational dashboard data is down.
/// A seller must always be able to change a required PIN, choose a language, or sign out.
class _ProfileUnavailable extends ConsumerWidget {
  const _ProfileUnavailable();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final translations = ref.watch(i18nBundleProvider);
    final session = ref.watch(userSessionProvider);

    return ListView(
      padding: const EdgeInsets.all(TchSpacing.s16),
      children: [
        _ProfileHeader(
          label:
              session.displayName ??
              session.username ??
              translations.translate('pos.profile.terminal'),
          ready: false,
        ),
        const SizedBox(height: TchSpacing.s16),
        FeedbackState(
          kind: FeedbackStateKind.offline,
          title: translations.translate('pos.profile.title'),
          message: translations.translate('common.error.network'),
        ),
        const SizedBox(height: TchSpacing.s24),
        FilledButton.icon(
          onPressed: () => context.go('/pos/settings'),
          icon: const Icon(Icons.settings_rounded),
          label: Text(translations.translate('pos.settings.title')),
        ),
      ],
    );
  }
}

class SellerTerminalSettingsPage extends ConsumerWidget {
  const SellerTerminalSettingsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profileAsync = ref.watch(posProfileProvider);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(title: Text(translations.translate('pos.settings.title'))),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(posProfileProvider);
            ref.invalidate(cashierHomeProvider);
          },
          child: profileAsync.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (_, _) => _SettingsBody(translations: translations),
            data: (profile) =>
                _SettingsBody(translations: translations, profile: profile),
          ),
        ),
      ),
    );
  }
}

class _SettingsBody extends ConsumerStatefulWidget {
  const _SettingsBody({required this.translations, this.profile});

  final I18nBundle translations;
  final PosProfileResponse? profile;

  @override
  ConsumerState<_SettingsBody> createState() => _SettingsBodyState();
}

class _SettingsBodyState extends ConsumerState<_SettingsBody> {
  String? _savingKey;

  Future<void> _save(
    String key, {
    bool? receiptAutoPrint,
    int? receiptCopyCount,
    bool? notificationsEnabled,
    bool? notificationsCriticalOnly,
  }) async {
    setState(() => _savingKey = key);
    try {
      await _updateSettings(
        ref,
        receiptAutoPrint: receiptAutoPrint,
        receiptCopyCount: receiptCopyCount,
        notificationsEnabled: notificationsEnabled,
        notificationsCriticalOnly: notificationsCriticalOnly,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(widget.translations.translate('pos.settings.saved')),
        ),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            widget.translations.translate('pos.settings.save_error'),
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _savingKey = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profile = widget.profile;
    final translations = widget.translations;
    final settings = profile?.settings;
    return ListView(
      padding: const EdgeInsets.all(TchSpacing.s16),
      children: [
        _ProfileHeader(
          label: translations.translate('pos.settings.title'),
          subtitle: profile?.terminal.displayName,
          ready: profile?.terminal.ready ?? false,
        ),
        const SizedBox(height: TchSpacing.s16),
        _Section(
          title: translations.translate('pos.settings.app'),
          children: [
            _LocaleSelector(
              label: translations.translate('pos.settings.language'),
              supportedLocales: settings?.supportedLocales,
            ),
            _InfoRow(
              icon: Icons.schedule_rounded,
              label: translations.translate('pos.settings.timezone'),
              value: _valueOrDash(settings?.timezone),
            ),
            _InfoRow(
              icon: Icons.info_outline_rounded,
              label: translations.translate('pos.settings.version'),
              value: _valueOrDash(profile?.version),
            ),
          ],
        ),
        const SizedBox(height: TchSpacing.s24),
        if (profile != null) ...[
          _Section(
            title: translations.translate('pos.settings.receipt'),
            children: [
              SwitchListTile(
                title: Text(translations.translate('pos.settings.auto_print')),
                value: settings?.receipt.autoPrint ?? true,
                onChanged: _savingKey == null
                    ? (value) =>
                          _save('receiptAutoPrint', receiptAutoPrint: value)
                    : null,
                secondary: _savingKey == 'receiptAutoPrint'
                    ? const _SettingProgress()
                    : const Icon(Icons.print_rounded),
              ),
              ListTile(
                leading: const Icon(Icons.copy_rounded),
                title: Text(translations.translate('pos.settings.copy_count')),
                subtitle: Text((settings?.receipt.copyCount ?? 1).toString()),
                trailing: SegmentedButton<int>(
                  showSelectedIcon: false,
                  segments: [
                    for (final value in const [1, 2, 3])
                      ButtonSegment(
                        value: value,
                        label: Text(value.toString()),
                      ),
                  ],
                  selected: {settings?.receipt.copyCount ?? 1},
                  onSelectionChanged: _savingKey == null
                      ? (selection) => _save(
                          'receiptCopyCount',
                          receiptCopyCount: selection.single,
                        )
                      : null,
                ),
              ),
            ],
          ),
          const SizedBox(height: TchSpacing.s24),
          _Section(
            title: translations.translate('pos.settings.notifications'),
            children: [
              SwitchListTile(
                title: Text(
                  translations.translate('pos.settings.notifications_enabled'),
                ),
                value: settings?.notifications.enabled ?? true,
                onChanged: _savingKey == null
                    ? (value) => _save(
                        'notificationsEnabled',
                        notificationsEnabled: value,
                      )
                    : null,
                secondary: _savingKey == 'notificationsEnabled'
                    ? const _SettingProgress()
                    : const Icon(Icons.notifications_active_rounded),
              ),
              SwitchListTile(
                title: Text(
                  translations.translate('pos.settings.critical_only'),
                ),
                value: settings?.notifications.criticalOnly ?? false,
                onChanged:
                    settings?.notifications.enabled == false ||
                        _savingKey != null
                    ? null
                    : (value) => _save(
                        'notificationsCriticalOnly',
                        notificationsCriticalOnly: value,
                      ),
                secondary: _savingKey == 'notificationsCriticalOnly'
                    ? const _SettingProgress()
                    : const Icon(Icons.priority_high_rounded),
              ),
            ],
          ),
          const SizedBox(height: TchSpacing.s24),
        ],
        _Section(
          title: translations.translate('pos.settings.security'),
          children: [
            ListTile(
              leading: const Icon(Icons.password_rounded),
              title: Text(translations.translate('pos.profile.change_pin')),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: () async {
                await context.push('/change-pin');
                ref.invalidate(posProfileProvider);
                ref.invalidate(cashierHomeProvider);
              },
            ),
          ],
        ),
        const SizedBox(height: TchSpacing.s24),
        FilledButton.icon(
          onPressed: () {
            ref.invalidate(posProfileProvider);
            ref.invalidate(cashierHomeProvider);
          },
          icon: const Icon(Icons.refresh_rounded),
          label: Text(translations.translate('pos.profile.refresh')),
        ),
        const SizedBox(height: TchSpacing.s12),
        OutlinedButton.icon(
          onPressed: () => showLogoutConfirmation(context, ref),
          icon: const Icon(Icons.logout_rounded),
          label: Text(translations.translate('pos.profile.sign_out')),
        ),
      ],
    );
  }
}

Future<void> _updateSettings(
  WidgetRef ref, {
  bool? receiptAutoPrint,
  int? receiptCopyCount,
  bool? notificationsEnabled,
  bool? notificationsCriticalOnly,
}) async {
  await ref
      .read(posProfileSettingsUpdaterProvider)
      .update(
        receiptAutoPrint: receiptAutoPrint,
        receiptCopyCount: receiptCopyCount,
        notificationsEnabled: notificationsEnabled,
        notificationsCriticalOnly: notificationsCriticalOnly,
      );
}

class _SettingProgress extends StatelessWidget {
  const _SettingProgress();

  @override
  Widget build(BuildContext context) => const SizedBox.square(
    dimension: 16,
    child: CircularProgressIndicator(strokeWidth: 2),
  );
}

class _LocaleSelector extends ConsumerWidget {
  const _LocaleSelector({required this.label, this.supportedLocales});

  final String label;
  final List<String>? supportedLocales;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final activeLocale = ref.watch(localeProvider);
    final filteredLocales =
        supportedLocales?.where(_isSupportedAppLocale).toList() ?? const [];
    final locales = filteredLocales.isEmpty
        ? const ['ht', 'fr', 'en']
        : filteredLocales;
    return ListTile(
      leading: const Icon(Icons.language_rounded),
      title: Text(label),
      subtitle: SegmentedButton<String>(
        showSelectedIcon: false,
        segments: [
          for (final locale in locales)
            ButtonSegment(value: locale, label: Text(_localeLabel(locale))),
        ],
        selected: {
          locales.contains(activeLocale) ? activeLocale : locales.first,
        },
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

bool _isSupportedAppLocale(String locale) => switch (locale) {
  'ht' || 'fr' || 'en' => true,
  _ => false,
};

String _localeLabel(String locale) => switch (locale) {
  'ht' => 'Kreyòl',
  'fr' => 'Français',
  'en' => 'English',
  _ => locale,
};

String _valueOrDash(String? value, [String fallback = '-']) {
  if (value == null || value.trim().isEmpty) return fallback;
  return value;
}

String _terminalStatusLabel(
  I18nBundle translations,
  PosProfileResponse profile,
) {
  if (profile.terminal.mustChangePin) {
    return translations.translate('pos.profile.status_change_pin');
  }
  final status = profile.terminal.status;
  if (status == null || status.isEmpty) return '-';
  return translations.translate('pos.profile.status_${status.toLowerCase()}');
}

String _commissionLabel(PosProfileCommercialInfo commercial) {
  final rate = commercial.commissionRate;
  if (rate == null) return '-';
  return '${rate.toString()}%';
}
