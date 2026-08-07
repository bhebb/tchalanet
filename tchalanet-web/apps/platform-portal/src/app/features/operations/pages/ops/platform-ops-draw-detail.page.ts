import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import {
  ConsoleDrawDetailComponent,
  ConsoleDrawDetailSectionActionEvent,
  ConsoleDrawDetailView,
  ConsoleEntityDetailActionEvent,
  ConsoleEntityDetailError,
  ConsoleEntityDetailState,
  ConsoleRowAction,
  consoleDrawDetailViewModel,
  consoleDrawLifecycleActionIcon,
  consoleDrawLifecycleActionLabel,
  consoleDrawResultStatusLabel,
  consoleDrawResultStatusTone,
  consoleDrawStatusLabel,
  consoleDrawStatusTone,
} from '@tch/web/console';

import {
  CancelDrawDialog,
  RescheduleDrawDialog,
  SimpleDrawActionDialog,
} from '../../components/dialogs/draw-action-dialogs';
import { ManualResultDialog as PlatformManualResultDialog } from '../../components/dialogs/manual-result.dialog';
import { OverrideResultDialog } from '../../components/dialogs/override-result.dialog';
import { DrawView, PlatformOpsApi } from '../../data-access/platform-ops-api.service';
import { DrawActionItem, actionsForDraw, drawResultOpsRow } from './platform-ops-draw-action-matrix';

export { actionsForDraw } from './platform-ops-draw-action-matrix';

@Component({
  selector: 'tch-platform-ops-draw-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConsoleDrawDetailComponent],
  templateUrl: './platform-ops-draw-detail.page.html',
})
export class PlatformOpsDrawDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);

  readonly state = signal<ConsoleEntityDetailState>('loading');
  readonly error = signal<ConsoleEntityDetailError | null>(null);
  readonly draw = signal<DrawView | null>(null);

  readonly pageActions: readonly ConsoleRowAction[] = [
    { id: 'back', label: 'Retour', icon: 'arrow_back' },
  ];

  readonly tenantId = computed(() => this.route.snapshot.paramMap.get('tenantId'));
  readonly drawId = computed(() => this.route.snapshot.paramMap.get('drawId'));

  readonly detailView = computed<ConsoleDrawDetailView | null>(() => {
    const draw = this.draw();
    if (!draw) return null;
    const resultNumbers = this.resultNumbers(draw);
    const lifecycleActions = actionsForDraw(draw).map(action => this.toConsoleDrawAction(action));

    return consoleDrawDetailViewModel({
      identityInput: {
        channelCode: draw.channel.code,
        channelName: draw.channel.name,
        slotKey: draw.slot.key,
        slotLabel: draw.slot.label,
        officialDateLabel: draw.drawDate,
        officialTimeLabel: this.drawTime(draw),
        officialTimezoneLabel: draw.slot.timezone,
        fallbackTitle: draw.channel.code,
      },
      title: draw.channel.name || draw.channel.code,
      description: `${draw.drawDate} · ${draw.slot.timezone ?? 'Fuseau non défini'}`,
      meta: [draw.channel.code, draw.slot.key, draw.status],
      statusLabel: consoleDrawStatusLabel(draw.status),
      statusTone: consoleDrawStatusTone(draw.status),
      overviewFacts: [
        { label: 'Tenant', value: draw.tenantId, code: true },
        { label: 'Canal', value: draw.channel.code, code: true },
        { label: 'Slot', value: draw.slot.key, code: true },
        { label: 'Date métier', value: draw.drawDate },
        { label: 'Planifié', value: this.dateTimeLabel(draw.scheduledAt, draw.slot.timezone) },
        { label: 'Fin des ventes', value: this.dateTimeLabel(draw.cutoffAt, draw.slot.timezone) },
        { label: 'Fuseau horaire', value: draw.slot.timezone ?? '—' },
        { label: 'Actif', value: draw.active ? 'Oui' : 'Non' },
      ],
      result: {
        statusLabel: draw.lastResult
          ? consoleDrawResultStatusLabel(draw.lastResult.status)
          : 'Résultat attendu',
        statusTone: draw.lastResult ? consoleDrawResultStatusTone(draw.lastResult.status) : 'warning',
        numbers: resultNumbers,
        modeLabel: draw.lastResult ? 'Normalisé' : null,
        fetchedAtLabel: draw.lastResult
          ? this.dateTimeLabel(draw.lastResult.occurredAt, draw.slot.timezone)
          : null,
        emptyLabel: 'Aucun résultat confirmé sur ce tirage.',
      },
      sections: [
        {
          id: 'lifecycle',
          title: 'Actions de cycle de vie',
          icon: 'settings',
          description: lifecycleActions.length
            ? 'Actions disponibles pour ce tirage dans le contexte tenant sélectionné.'
            : 'Aucune action disponible pour ce statut.',
          actions: lifecycleActions,
        },
      ],
      aside: {
        title: draw.channel.name || draw.channel.code,
        subtitle: this.dateTimeLabel(draw.scheduledAt, draw.slot.timezone),
        code: draw.slot.key,
        items: [
          { label: 'Statut', value: consoleDrawStatusLabel(draw.status) },
          { label: 'Résultat', value: draw.lastResult ? consoleDrawResultStatusLabel(draw.lastResult.status) : 'Attendu' },
          { label: 'Canal', value: draw.channel.code },
          { label: 'Tenant', value: draw.tenantId },
        ],
        advice: 'Vue plateforme: mêmes composants que le tenant admin, avec actions exécutées dans le contexte tenant.',
      },
    });
  });

  constructor() {
    this.load();
  }

  load(): void {
    const drawId = this.drawId();
    const tenantId = this.tenantId();
    if (!drawId || !tenantId) {
      this.state.set('error');
      this.error.set({
        title: 'Tirage introuvable',
        message: 'Le tenant ou le tirage est absent de l’URL.',
      });
      return;
    }

    this.state.set('loading');
    this.error.set(null);
    this.api.getDraw(drawId, tenantId, { suppressShellFeedback: true }).subscribe({
      next: draw => {
        this.draw.set(draw);
        this.state.set('ready');
      },
      error: () => {
        this.state.set('error');
        this.error.set({
          title: 'Impossible de charger le tirage',
          message: 'Réessayez ou revenez à la liste des tirages.',
        });
      },
    });
  }

  onPageAction(event: ConsoleEntityDetailActionEvent): void {
    if (event.action.id === 'back') {
      void this.router.navigate(['/app/platform/ops/draws']);
    }
  }

  onSectionAction(event: ConsoleDrawDetailSectionActionEvent): void {
    const draw = this.draw();
    if (!draw) return;
    this.openRowAction(draw, { kind: event.action.id } as DrawActionItem);
  }

  private openRowAction(draw: DrawView, action: DrawActionItem): void {
    const tenantId = this.tenantId() ?? draw.tenantId ?? null;
    switch (action.kind) {
      case 'cancel':
        this.openAndReload(CancelDrawDialog, { draw, tenantId }, '460px');
        break;
      case 'open':
      case 'close':
      case 'lock':
      case 'unlock':
      case 'settle':
      case 'archive':
        this.openAndReload(SimpleDrawActionDialog, { draw, action: action.kind, tenantId }, '440px');
        break;
      case 'reschedule':
        this.openAndReload(RescheduleDrawDialog, { draw, tenantId }, '480px');
        break;
      case 'manual':
        this.openAndReload(
          PlatformManualResultDialog,
          { drawDate: draw.drawDate, slotKey: draw.slot.key },
          '520px',
        );
        break;
      case 'confirm':
        this.confirmResult(draw);
        break;
      case 'override':
        {
          const row = drawResultOpsRow(draw);
          if (!row) return;
          this.openAndReload(
            OverrideResultDialog,
            { row, onSuccess: () => this.load() },
            '500px',
          );
        }
        break;
    }
  }

  private confirmResult(draw: DrawView): void {
    const resultId = draw.lastResult?.id;
    if (!resultId) return;
    this.api.confirmDrawResult(resultId, { suppressShellFeedback: true }).subscribe({
      next: () => this.load(),
      error: () => {
        this.error.set({
          title: 'Impossible de confirmer le résultat',
          message: 'Actualisez le tirage puis réessayez.',
        });
      },
    });
  }

  private openAndReload(component: unknown, data: unknown, width: string): void {
    const ref = this.dialog.open(component as Parameters<MatDialog['open']>[0], { data, width });
    ref.afterClosed().subscribe((done: boolean | null) => {
      if (done) this.load();
    });
  }

  private toConsoleDrawAction(action: DrawActionItem): ConsoleRowAction {
    if (action.kind === 'manual') {
      return {
        id: action.kind,
        label: 'Entrer le résultat',
        icon: 'edit_note',
        tone: 'default',
        variant: 'button',
      };
    }
    if (action.kind === 'confirm') {
      return {
        id: action.kind,
        label: 'Confirmer le résultat',
        icon: 'check_circle',
        tone: 'primary',
        variant: 'button',
      };
    }
    if (action.kind === 'override') {
      return {
        id: action.kind,
        label: 'Corriger le résultat',
        icon: 'edit',
        tone: 'danger',
        variant: 'button',
      };
    }
    return {
      id: action.kind,
      label: consoleDrawLifecycleActionLabel(action.kind),
      icon: consoleDrawLifecycleActionIcon(action.kind),
      tone: action.kind === 'cancel' ? 'danger' : 'default',
      variant: 'button',
    };
  }

  private resultNumbers(draw: DrawView): string[] {
    const result = draw.lastResult;
    if (!result) return [];
    return [result.lot1, result.lot2, result.lot3, result.lot4]
      .map(value => typeof value === 'string' || typeof value === 'number' ? String(value).trim() : '')
      .filter(Boolean);
  }

  private drawTime(draw: DrawView): string | undefined {
    return this.normalizeTime(draw.slot.drawTime) ?? this.timeLabel(draw.scheduledAt, draw.slot.timezone);
  }

  private dateTimeLabel(value: string | null | undefined, timezone?: string | null): string {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat('fr-CA', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: timezone || undefined,
    }).format(date);
  }

  private timeLabel(value: string | null | undefined, timezone?: string | null): string | undefined {
    if (!value) return undefined;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return undefined;
    return new Intl.DateTimeFormat('fr-CA', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: timezone || undefined,
    }).format(date);
  }

  private normalizeTime(value: string | null | undefined): string | undefined {
    const match = value?.trim().match(/^(\d{1,2}):(\d{2})/);
    if (!match) return undefined;
    return `${match[1].padStart(2, '0')}:${match[2]}`;
  }
}
