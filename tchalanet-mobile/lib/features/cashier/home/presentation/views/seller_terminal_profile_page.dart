import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/i18n/i18n_models.dart';
import '../../../../../core/i18n/i18n_repository.dart';
import '../../../../../design_system/components/components.dart';
import '../../../../../design_system/tokens/tch_radius.dart';
import '../../../../../design_system/tokens/tch_spacing.dart';
import '../../../../auth/presentation/view_models/auth_controller.dart';
import '../../../tickets/printing/bluetooth_esc_pos_printer.dart';
import '../../../tickets/printing/printer_service.dart';
import '../../../tickets/printing/sunmi_internal_printer.dart';
import '../../data/models/pos_profile_models.dart';
import '../view_models/cashier_home_providers.dart';

class SellerTerminalProfilePage extends ConsumerWidget {
  const SellerTerminalProfilePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profileAsync = ref.watch(posProfileProvider);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          tooltip: MaterialLocalizations.of(context).backButtonTooltip,
          onPressed: () => context.go('/pos'),
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(translations.translate('pos.profile.title')),
      ),
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
      appBar: AppBar(
        leading: IconButton(
          tooltip: MaterialLocalizations.of(context).backButtonTooltip,
          onPressed: () => context.go('/pos'),
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(translations.translate('pos.settings.title')),
      ),
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
    bool? receiptQuickSale,
    String? receiptPrinterMode,
    String? receiptPaperSize,
    String? receiptAdapterPreference,
    bool? notificationsEnabled,
    bool? notificationsCriticalOnly,
  }) async {
    setState(() => _savingKey = key);
    try {
      await _updateSettings(
        ref,
        receiptAutoPrint: receiptAutoPrint,
        receiptCopyCount: receiptCopyCount,
        receiptQuickSale: receiptQuickSale,
        receiptPrinterMode: receiptPrinterMode,
        receiptPaperSize: receiptPaperSize,
        receiptAdapterPreference: receiptAdapterPreference,
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
              SwitchListTile(
                title: Text(translations.translate('pos.settings.quick_sale')),
                subtitle: Text(
                  translations.translate('pos.settings.quick_sale_help'),
                ),
                value: settings?.receipt.quickSale ?? true,
                onChanged: _savingKey == null
                    ? (value) =>
                          _save('receiptQuickSale', receiptQuickSale: value)
                    : null,
                secondary: _savingKey == 'receiptQuickSale'
                    ? const _SettingProgress()
                    : const Icon(Icons.flash_on_rounded),
              ),
              ListTile(
                leading: const Icon(Icons.print_outlined),
                title: Text(
                  translations.translate('pos.settings.printer_mode'),
                ),
                subtitle: Text(settings?.receipt.printerMode ?? 'POS_DIRECT'),
                trailing: DropdownButton<String>(
                  value: settings?.receipt.printerMode ?? 'POS_DIRECT',
                  items: [
                    DropdownMenuItem(
                      value: 'AUTO',
                      child: Text(
                        translations.translate(
                          'pos.settings.printer_mode_auto',
                        ),
                      ),
                    ),
                    DropdownMenuItem(
                      value: 'POS_DIRECT',
                      child: Text(
                        translations.translate('pos.settings.printer_mode_pos'),
                      ),
                    ),
                    DropdownMenuItem(
                      value: 'SYSTEM_PDF',
                      child: Text(
                        translations.translate('pos.settings.printer_mode_pdf'),
                      ),
                    ),
                  ],
                  onChanged: _savingKey == null && settings != null
                      ? (value) {
                          if (value != null) {
                            _save(
                              'receiptPrinterMode',
                              receiptPrinterMode: value,
                            );
                          }
                        }
                      : null,
                ),
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
              ListTile(
                leading: const Icon(Icons.receipt_long_rounded),
                title: Text(translations.translate('pos.settings.paper_size')),
                subtitle: Text(settings?.receipt.paperSize ?? 'RECEIPT_58MM'),
                trailing: DropdownButton<String>(
                  value: settings?.receipt.paperSize ?? 'RECEIPT_58MM',
                  items: [
                    DropdownMenuItem(
                      value: 'RECEIPT_58MM',
                      child: Text(
                        translations.translate('pos.settings.paper_58mm'),
                      ),
                    ),
                    DropdownMenuItem(
                      value: 'RECEIPT_80MM',
                      child: Text(
                        translations.translate('pos.settings.paper_80mm'),
                      ),
                    ),
                  ],
                  onChanged: _savingKey == null && settings != null
                      ? (value) {
                          if (value != null) {
                            _save('receiptPaperSize', receiptPaperSize: value);
                          }
                        }
                      : null,
                ),
              ),
              _BluetoothPrinterSettings(translations: translations),
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

class _BluetoothPrinterSettings extends ConsumerStatefulWidget {
  const _BluetoothPrinterSettings({required this.translations});

  final I18nBundle translations;

  @override
  ConsumerState<_BluetoothPrinterSettings> createState() =>
      _BluetoothPrinterSettingsState();
}

class _BluetoothPrinterSettingsState
    extends ConsumerState<_BluetoothPrinterSettings> {
  List<BluetoothPrinterDevice> _devices = const [];
  BluetoothPrinterDevice? _selected;
  bool _sunmiAvailable = false;
  bool _busy = false;
  String? _error;

  BluetoothEscPosPrinterRepository get _repository =>
      ref.read(bluetoothPrinterRepositoryProvider);

  @override
  void initState() {
    super.initState();
    Future<void>(() async {
      final selected = await _repository.selected();
      final sunmiAvailable = await const SunmiInternalPrinterAdapter()
          .isAvailable();
      if (mounted) {
        setState(() {
          _selected = selected;
          _sunmiAvailable = sunmiAvailable;
        });
      }
    });
  }

  Future<void> _scan() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final devices = await _repository.scan();
      if (mounted) setState(() => _devices = devices);
    } catch (error) {
      if (mounted) setState(() => _error = _printerErrorMessage());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _select(BluetoothPrinterDevice device) async {
    setState(() => _busy = true);
    try {
      await _repository.select(device);
      if (!mounted) return;
      setState(() {
        _selected = device;
        _error = null;
      });
      ref.invalidate(printerServiceProvider);
    } catch (error) {
      if (mounted) setState(() => _error = _printerErrorMessage());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _test() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final bytes = Uint8List.fromList(const [
        0x1b, 0x40, // initialize
        0x1b, 0x61, 0x01, // center
        0x1b, 0x45, 0x01, // bold
        84, 99, 104, 97, 108, 97, 110, 101, 116, 10, // Tchalanet
        0x1b, 0x45, 0x00,
        80, 114, 105, 110, 116, 32, 79, 75, 10, 10,
        0x1d, 0x56, 0x00, // full cut when supported
      ]);
      if (_sunmiAvailable) {
        await const SunmiInternalPrinterAdapter().print(bytes);
      } else {
        await _repository.write(bytes);
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              widget.translations.translate('pos.settings.printer_test_ok'),
            ),
          ),
        );
      }
    } catch (error) {
      if (mounted) setState(() => _error = _printerErrorMessage());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _printerErrorMessage() =>
      widget.translations.translate('pos.settings.printer_error');

  Future<void> _clear() async {
    await _repository.clear();
    if (!mounted) return;
    setState(() => _selected = null);
    ref.invalidate(printerServiceProvider);
  }

  @override
  Widget build(BuildContext context) {
    final translations = widget.translations;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ListTile(
          contentPadding: EdgeInsets.zero,
          leading: const Icon(Icons.bluetooth_rounded),
          title: Text(translations.translate('pos.settings.printer')),
          subtitle: Text(
            _sunmiAvailable
                ? 'Sunmi internal printer'
                : _selected?.name ??
                      translations.translate(
                        'pos.settings.printer_not_configured',
                      ),
          ),
        ),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: _busy ? null : _scan,
                icon: const Icon(Icons.search_rounded),
                label: Text(
                  translations.translate('pos.settings.search_printer'),
                ),
              ),
            ),
            if (_selected != null) ...[
              const SizedBox(width: TchSpacing.s8),
              IconButton(
                tooltip: translations.translate('pos.settings.forget_printer'),
                onPressed: _busy ? null : _clear,
                icon: const Icon(Icons.delete_outline_rounded),
              ),
            ],
          ],
        ),
        if (_devices.isNotEmpty)
          ..._devices.map(
            (device) => ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Icon(
                _selected?.id == device.id
                    ? Icons.radio_button_checked_rounded
                    : Icons.radio_button_unchecked_rounded,
              ),
              title: Text(device.name),
              subtitle: Text(device.id),
              onTap: _busy ? null : () => _select(device),
            ),
          ),
        if (_sunmiAvailable || _selected != null)
          OutlinedButton.icon(
            onPressed: _busy ? null : _test,
            icon: const Icon(Icons.print_rounded),
            label: Text(translations.translate('pos.settings.test_printer')),
          ),
        if (_error != null)
          Text(
            _error!,
            style: TextStyle(color: Theme.of(context).colorScheme.error),
          ),
      ],
    );
  }
}

Future<void> _updateSettings(
  WidgetRef ref, {
  bool? receiptAutoPrint,
  int? receiptCopyCount,
  bool? receiptQuickSale,
  String? receiptPrinterMode,
  String? receiptPaperSize,
  String? receiptAdapterPreference,
  bool? notificationsEnabled,
  bool? notificationsCriticalOnly,
}) async {
  await ref
      .read(posProfileSettingsUpdaterProvider)
      .update(
        receiptAutoPrint: receiptAutoPrint,
        receiptCopyCount: receiptCopyCount,
        receiptQuickSale: receiptQuickSale,
        receiptPrinterMode: receiptPrinterMode,
        receiptPaperSize: receiptPaperSize,
        receiptAdapterPreference: receiptAdapterPreference,
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
