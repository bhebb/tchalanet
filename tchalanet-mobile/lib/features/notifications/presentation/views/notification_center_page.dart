import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/i18n/i18n_models.dart';
import '../../../../core/i18n/i18n_repository.dart';
import '../../../../design_system/components/feedback_state.dart';
import '../../../../design_system/components/status_badge.dart';
import '../../../../design_system/components/surface_card.dart';
import '../../../../design_system/tokens/tch_spacing.dart';
import '../../data/models/notification_models.dart';
import '../view_models/notification_center_view_model.dart';

class NotificationCenterPage extends ConsumerWidget {
  const NotificationCenterPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(notificationCenterProvider);
    final viewModel = ref.read(notificationCenterProvider.notifier);
    final translations = ref.watch(i18nBundleProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(translations.translate('notifications.center.title')),
        actions: [
          IconButton(
            tooltip: translations.translate('notifications.center.refresh'),
            onPressed: state.loading ? null : viewModel.refresh,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: Column(
        children: [
          _StatusFilter(
            selected: state.status,
            translations: translations,
            onSelected: viewModel.selectStatus,
          ),
          if (state.errorKey != null && state.items.isNotEmpty)
            MaterialBanner(
              content: Text(translations.translate(state.errorKey!)),
              actions: [
                TextButton(
                  onPressed: viewModel.refresh,
                  child: Text(translations.translate('common.retry')),
                ),
              ],
            ),
          Expanded(
            child: _NotificationContent(
              state: state,
              translations: translations,
              onRetry: viewModel.refresh,
              onLoadMore: viewModel.loadMore,
              onRead: viewModel.markRead,
              onArchive: viewModel.archive,
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusFilter extends StatelessWidget {
  const _StatusFilter({
    required this.selected,
    required this.translations,
    required this.onSelected,
  });

  final NotificationStatus selected;
  final I18nBundle translations;
  final ValueChanged<NotificationStatus> onSelected;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(
        horizontal: TchSpacing.s16,
        vertical: TchSpacing.s8,
      ),
      child: SegmentedButton<NotificationStatus>(
        segments: [
          ButtonSegment(
            value: NotificationStatus.unread,
            icon: const Icon(Icons.mark_email_unread_outlined),
            label: Text(translations.translate('notifications.status.unread')),
          ),
          ButtonSegment(
            value: NotificationStatus.read,
            icon: const Icon(Icons.drafts_outlined),
            label: Text(translations.translate('notifications.status.read')),
          ),
          ButtonSegment(
            value: NotificationStatus.archived,
            icon: const Icon(Icons.archive_outlined),
            label: Text(
              translations.translate('notifications.status.archived'),
            ),
          ),
        ],
        selected: {selected},
        onSelectionChanged: (selection) => onSelected(selection.single),
      ),
    );
  }
}

class _NotificationContent extends StatelessWidget {
  const _NotificationContent({
    required this.state,
    required this.translations,
    required this.onRetry,
    required this.onLoadMore,
    required this.onRead,
    required this.onArchive,
  });

  final NotificationCenterState state;
  final I18nBundle translations;
  final VoidCallback onRetry;
  final VoidCallback onLoadMore;
  final ValueChanged<String> onRead;
  final ValueChanged<String> onArchive;

  @override
  Widget build(BuildContext context) {
    if (state.loading && state.items.isEmpty) {
      return FeedbackState(
        kind: FeedbackStateKind.loading,
        title: translations.translate('common.loading'),
      );
    }
    if (state.errorKey != null && state.items.isEmpty) {
      return FeedbackState(
        kind: FeedbackStateKind.error,
        title: translations.translate('notifications.center.load_error'),
        actionLabel: translations.translate('common.retry'),
        onAction: onRetry,
      );
    }
    if (state.items.isEmpty) {
      return FeedbackState(
        kind: FeedbackStateKind.empty,
        title: translations.translate('notifications.center.empty'),
        message: translations.translate(
          'notifications.center.empty.${state.status.name}',
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () async => onRetry(),
      child: ListView.separated(
        padding: const EdgeInsets.all(TchSpacing.s16),
        itemCount: state.items.length + (state.hasNext ? 1 : 0),
        separatorBuilder: (_, _) => const SizedBox(height: TchSpacing.s12),
        itemBuilder: (context, index) {
          if (index == state.items.length) {
            return Center(
              child: TextButton.icon(
                onPressed: state.loadingMore ? null : onLoadMore,
                icon: state.loadingMore
                    ? const SizedBox.square(
                        dimension: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.expand_more_rounded),
                label: Text(
                  translations.translate('notifications.center.load_more'),
                ),
              ),
            );
          }
          final item = state.items[index];
          return _NotificationCard(
            item: item,
            translations: translations,
            busy: state.busyIds.contains(item.id),
            onRead: () => onRead(item.id),
            onArchive: () => onArchive(item.id),
          );
        },
      ),
    );
  }
}

class _NotificationCard extends StatelessWidget {
  const _NotificationCard({
    required this.item,
    required this.translations,
    required this.busy,
    required this.onRead,
    required this.onArchive,
  });

  final NotificationItem item;
  final I18nBundle translations;
  final bool busy;
  final VoidCallback onRead;
  final VoidCallback onArchive;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SurfaceCard(
      emphasis: item.status == NotificationStatus.unread
          ? SurfaceCardEmphasis.normal
          : SurfaceCardEmphasis.lowest,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(_icon, color: _color(context)),
              const SizedBox(width: TchSpacing.s12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: item.status == NotificationStatus.unread
                            ? FontWeight.w800
                            : FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: TchSpacing.s4),
                    Text(
                      _message,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: scheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: TchSpacing.s8),
              StatusBadge(
                label: translations.translate(
                  'notifications.severity.${item.severity.name}',
                ),
                kind: _badgeKind,
              ),
            ],
          ),
          const SizedBox(height: TchSpacing.s12),
          Text(
            _timestamp(item.createdAt),
            style: Theme.of(
              context,
            ).textTheme.labelSmall?.copyWith(color: scheme.outline),
          ),
          if (item.status != NotificationStatus.archived) ...[
            const SizedBox(height: TchSpacing.s12),
            Wrap(
              spacing: TchSpacing.s8,
              runSpacing: TchSpacing.s8,
              children: [
                if (item.status == NotificationStatus.unread)
                  TextButton.icon(
                    onPressed: busy ? null : onRead,
                    icon: const Icon(Icons.done_rounded),
                    label: Text(
                      translations.translate('notifications.action.mark_read'),
                    ),
                  ),
                TextButton.icon(
                  onPressed: busy ? null : onArchive,
                  icon: const Icon(Icons.archive_outlined),
                  label: Text(
                    translations.translate('notifications.action.archive'),
                  ),
                ),
                if (_hasRoutableAction)
                  TextButton.icon(
                    onPressed: busy
                        ? null
                        : () => context.push(item.action!.url),
                    icon: const Icon(Icons.open_in_new_rounded),
                    label: Text(
                      translations.translate('notifications.action.open'),
                    ),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  String get _title => item.titleKey == null
      ? item.titleText ?? ''
      : translations.translate(item.titleKey!, fallback: item.titleText);

  String get _message => item.messageKey == null
      ? item.messageText ?? ''
      : translations.translate(item.messageKey!, fallback: item.messageText);

  bool get _hasRoutableAction =>
      item.action?.type.toUpperCase() == 'ROUTE' &&
      item.action!.url.startsWith('/');

  IconData get _icon => switch (item.severity) {
    NotificationSeverity.info => Icons.info_outline_rounded,
    NotificationSeverity.warning => Icons.warning_amber_rounded,
    NotificationSeverity.error => Icons.error_outline_rounded,
    NotificationSeverity.critical => Icons.crisis_alert_rounded,
  };

  Color _color(BuildContext context) => switch (item.severity) {
    NotificationSeverity.info => Theme.of(context).colorScheme.primary,
    NotificationSeverity.warning => Theme.of(context).colorScheme.tertiary,
    NotificationSeverity.error ||
    NotificationSeverity.critical => Theme.of(context).colorScheme.error,
  };

  StatusBadgeKind get _badgeKind => switch (item.severity) {
    NotificationSeverity.info => StatusBadgeKind.neutral,
    NotificationSeverity.warning => StatusBadgeKind.warning,
    NotificationSeverity.error ||
    NotificationSeverity.critical => StatusBadgeKind.blocked,
  };

  String _timestamp(DateTime value) {
    final local = value.toLocal();
    String two(int part) => part.toString().padLeft(2, '0');
    return '${local.year}-${two(local.month)}-${two(local.day)} '
        '${two(local.hour)}:${two(local.minute)}';
  }
}
