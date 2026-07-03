import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  CatalogDrawChannelGameView,
  CatalogDrawChannelView,
  CatalogId,
  PlatformCatalogApi,
} from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-platform-catalog-draw-channel-games-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-draw-channel-games.page.html',
})
export class PlatformCatalogDrawChannelGamesPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly channelColumns = ['tenantGameId', 'enabled', 'flags', 'actions'];
  readonly channels = this.api.listDrawChannelsResource(() => ({ size: 200 }), {
    suppressShellFeedback: true,
  });
  readonly channelError = resourceErrorVm(this.channels, 'platform.catalog.drawChannelGames.channels');
  readonly channelRows = computed(() => this.channels.value()?.items ?? []);

  readonly selectedChannelId = signal<string>('');
  readonly tenantGameId = signal('');
  readonly enabled = signal(true);
  readonly flagsJson = signal('{}');
  readonly busy = signal(false);

  readonly games = this.api.listDrawChannelGamesResource(
    () => this.selectedChannelId() || undefined,
    { suppressShellFeedback: true },
  );
  readonly gamesError = resourceErrorVm(this.games, 'platform.catalog.drawChannelGames');
  readonly gameRows = computed(() => this.games.value() ?? []);

  selectChannel(id: string): void {
    this.selectedChannelId.set(id);
  }

  reload(): void {
    this.channels.reload();
    this.games.reload();
  }

  upsert(): void {
    const channelId = this.selectedChannelId();
    const tenantGameId = this.tenantGameId().trim();
    if (!channelId || !tenantGameId) return;

    const flags = parseJson(this.flagsJson(), this.snackBar);
    if (flags === INVALID_JSON) return;

    this.busy.set(true);
    this.api.upsertDrawChannelGame(channelId, {
      tenantGameId,
      enabled: this.enabled(),
      flags,
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.tenantGameId.set('');
        this.flagsJson.set('{}');
        this.snackBar.open('Association enregistrée.', 'OK', { duration: 4000 });
        this.games.reload();
      },
      error: err => this.handleError(err),
    });
  }

  toggle(row: CatalogDrawChannelGameView): void {
    const channelId = this.selectedChannelId();
    const tenantGameId = idValue(row.tenantGameId);
    if (!channelId || !tenantGameId) return;

    this.busy.set(true);
    this.api.updateDrawChannelGame(channelId, tenantGameId, { enabled: !row.enabled }).subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open('Association mise à jour.', 'OK', { duration: 4000 });
        this.games.reload();
      },
      error: err => this.handleError(err),
    });
  }

  editFlags(row: CatalogDrawChannelGameView): void {
    const channelId = this.selectedChannelId();
    const tenantGameId = idValue(row.tenantGameId);
    if (!channelId || !tenantGameId) return;

    const nextValue = window.prompt('Flags JSON', formatJson(row.flags));
    if (nextValue == null) return;
    const flags = parseJson(nextValue, this.snackBar);
    if (flags === INVALID_JSON) return;

    this.busy.set(true);
    this.api.updateDrawChannelGame(channelId, tenantGameId, { flags }).subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open('Flags mis à jour.', 'OK', { duration: 4000 });
        this.games.reload();
      },
      error: err => this.handleError(err),
    });
  }

  delete(row: CatalogDrawChannelGameView): void {
    const channelId = this.selectedChannelId();
    const tenantGameId = idValue(row.tenantGameId);
    if (!channelId || !tenantGameId) return;
    if (!confirm(`Retirer le jeu ${tenantGameId} de ce canal ?`)) return;

    this.busy.set(true);
    this.api.deleteDrawChannelGame(channelId, tenantGameId).subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open('Association supprimée.', 'OK', { duration: 4000 });
        this.games.reload();
      },
      error: err => this.handleError(err),
    });
  }

  channelLabel(channel: CatalogDrawChannelView): string {
    return `${channel.name} (${channel.code})`;
  }

  idText(id: CatalogId): string {
    return idValue(id);
  }

  flagsText(flags: unknown): string {
    return formatJson(flags);
  }

  private handleError(err: unknown): void {
    this.busy.set(false);
    this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', {
      duration: 5000,
    });
  }
}

const INVALID_JSON = Symbol('invalid-json');

function idValue(id: CatalogId): string {
  return typeof id === 'string' ? id : id.value;
}

function parseJson(raw: string, snackBar: MatSnackBar): unknown | typeof INVALID_JSON {
  const text = raw.trim();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    snackBar.open('JSON invalide.', 'OK', { duration: 5000 });
    return INVALID_JSON;
  }
}

function formatJson(value: unknown): string {
  if (value == null) return '{}';
  if (typeof value === 'string') return value;
  return JSON.stringify(value);
}
