import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { TchPage } from '@tch/api';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import { TchPaginationComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  ConsoleDrawSlotIdentity,
  ConsoleDrawSlotIdentityComponent,
  consoleDrawIdentity,
} from '@tch/web/console';
import { CreateDrawChannelDialog } from '../../components/dialogs/create-draw-channel.dialog';
import {
  PlatformCatalogApi,
  CatalogDrawChannelView,
  UpdateDrawChannelRequest,
} from '../../data-access/platform-catalog-api.service';

type CatalogDrawChannelRow = CatalogDrawChannelView & {
  readonly identity: ConsoleDrawSlotIdentity;
  readonly drawTimeLabel: string;
  readonly daysLabel: string;
  readonly statusLabel: string;
  readonly statusTone: 'success' | 'neutral';
};

// ── Main Page ────────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-platform-catalog-draw-channels-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    ConsoleDrawSlotIdentityComponent,
    TchPaginationComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-draw-channels.page.html',
  styleUrls: ['./platform-catalog-draw-channels.page.scss'],
})
export class PlatformCatalogDrawChannelsPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly displayedColumns = [
    'identity',
    'drawTime',
    'timezone',
    'daysOfWeek',
    'sortOrder',
    'active',
    'actions',
  ];

  readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  readonly search = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() => numberParam(this.queryParamMap().get('size'), 20));

  readonly channels = this.api.listDrawChannelsResource(
    () => ({
      q: this.search() || undefined,
      page: this.page(),
      size: this.size(),
    }),
    { suppressShellFeedback: true },
  );
  readonly channelsError = resourceErrorVm(this.channels, 'platform.catalog.drawChannels');
  readonly channelPage = computed<TchPage<CatalogDrawChannelView> | null>(() => {
    const status = this.channels.status();
    if (status !== 'resolved' && status !== 'local' && status !== 'reloading') return null;
    return this.channels.value() ?? null;
  });
  readonly channelRows = computed<readonly CatalogDrawChannelRow[]>(() =>
    (this.channelPage()?.items ?? []).map(channel => this.toRow(channel)),
  );
  readonly hasChannels = computed(() => this.channelRows().length > 0);
  readonly totalElements = computed(() => this.channelPage()?.totalElements ?? 0);
  readonly pageIndex = computed(() => this.channelPage()?.page ?? this.page());
  readonly pageSize = computed(() => this.channelPage()?.size ?? this.size());
  readonly selectedChannel = signal<CatalogDrawChannelRow | null>(null);
  readonly savingConfig = signal(false);

  readonly configForm = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    label: [''],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    timezone: ['', Validators.required],
    daysOfWeek: [''],
    cutoffSec: [null as number | null],
    sortOrder: [10],
    period: [''],
    resultSlotId: [''],
    defaultSource: [''],
    notes: [''],
    active: [true],
  });

  private showError(msg: string): void {
    this.snackBar.open(msg, 'OK', { duration: 5000 });
  }

  load(): void {
    this.channels.reload();
  }

  onSearch(v: string): void {
    const value = v.trim();
    this.navigateList({ q: value || null, page: null });
  }

  onPageChange(page: number): void {
    this.navigateList({ page: page > 0 ? page : null });
  }

  onSizeChange(size: number): void {
    this.navigateList({ size: size !== 20 ? size : null, page: null });
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateDrawChannelDialog, { width: '520px' });
    ref.afterClosed().subscribe((created: CatalogDrawChannelView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Canal créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openConfig(ch: CatalogDrawChannelRow): void {
    this.selectedChannel.set(ch);
    this.configForm.reset({
      code: ch.code,
      name: ch.name,
      label: ch.label ?? '',
      drawTime: ch.drawTime?.substring(0, 5) ?? '',
      timezone: ch.timezone ?? '',
      daysOfWeek: ch.daysOfWeek?.join(',') ?? '',
      cutoffSec: ch.cutoffSec ?? null,
      sortOrder: ch.sortOrder,
      period: ch.period ?? '',
      resultSlotId: ch.resultSlotId ?? '',
      defaultSource: ch.defaultSource ?? '',
      notes: ch.notes ?? '',
      active: ch.active,
    });
  }

  closeConfig(): void {
    this.selectedChannel.set(null);
  }

  saveConfig(): void {
    const channel = this.selectedChannel();
    if (!channel || this.configForm.invalid || this.savingConfig()) return;

    this.savingConfig.set(true);
    const v = this.configForm.getRawValue();
    const days = v.daysOfWeek
      ? v.daysOfWeek
          .split(',')
          .map(d => d.trim().toUpperCase())
          .filter(Boolean)
      : null;
    const req: UpdateDrawChannelRequest = {
      code: v.code.toUpperCase(),
      name: v.name,
      label: v.label || null,
      drawTime: v.drawTime,
      timezone: v.timezone || null,
      daysOfWeek: days?.length ? days : null,
      cutoffSec: v.cutoffSec ?? null,
      sortOrder: v.sortOrder,
      period: v.period || null,
      resultSlotId: v.resultSlotId || null,
      defaultSource: v.defaultSource || null,
      notes: v.notes || null,
      active: v.active,
    };
    this.api.updateDrawChannel(channel.id, req).subscribe({
      next: updated => {
        this.savingConfig.set(false);
        this.snackBar.open('Canal mis à jour.', 'OK', { duration: 4000 });
        this.openConfig(this.toRow(updated));
        this.load();
      },
      error: (err: unknown) => {
        this.savingConfig.set(false);
        this.showError((err as { error?: { detail?: string; title?: string } })?.error?.detail
          ?? (err as { error?: { title?: string } })?.error?.title
          ?? 'Erreur.');
      },
    });
  }

  disable(ch: CatalogDrawChannelView): void {
    if (!confirm(`Désactiver le canal « ${ch.name} » ?`)) return;
    this.api.disableDrawChannel(ch.id).subscribe({
      next: () => {
        this.snackBar.open('Canal désactivé.', 'OK', { duration: 4000 });
        this.load();
      },
      error: (err: unknown) => {
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.',
          'OK',
          { duration: 5000 },
        );
      },
    });
  }

  delete(ch: CatalogDrawChannelView): void {
    if (!confirm(`Supprimer le canal « ${ch.name} » ?`)) return;
    this.api.deleteDrawChannel(ch.id).subscribe({
      next: () => {
        this.snackBar.open('Canal supprimé.', 'OK', { duration: 4000 });
        this.load();
      },
      error: (err: unknown) => {
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.',
          'OK',
          { duration: 5000 },
        );
      },
    });
  }

  private toRow(channel: CatalogDrawChannelView): CatalogDrawChannelRow {
    return {
      ...channel,
      identity: consoleDrawIdentity({
        channelCode: channel.code,
        channelName: channel.name,
        channelShortName: channel.label,
        slotKey: channel.code,
        officialTimeLabel: this.timeLabel(channel.drawTime),
        officialTimezoneLabel: channel.timezone,
      }),
      drawTimeLabel: this.timeLabel(channel.drawTime) || '—',
      daysLabel: channel.daysOfWeek?.length ? channel.daysOfWeek.join(', ') : '—',
      statusLabel: channel.active ? 'Actif' : 'Inactif',
      statusTone: channel.active ? 'success' : 'neutral',
    };
  }

  private timeLabel(time: string | null | undefined): string {
    return time ? time.substring(0, 5) : '';
  }

  private navigateList(params: {
    readonly q?: string | null;
    readonly page?: number | null;
    readonly size?: number | null;
  }): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }
}

function numberParam(value: string | null, fallback: number): number {
  if (value === null || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}
