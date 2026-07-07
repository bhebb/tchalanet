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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchPage } from '@tch/api';
import { TchConfirmDialog, TchConfirmDialogData } from '@tch/ui/components';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { TchPaginationComponent } from '@tch/ui/console';
import {
  consoleGameName,
  ConsoleGameActionEvent,
  ConsoleGameRow,
  ConsoleGamesTableComponent,
} from '@tch/web/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  PlatformCatalogApi,
  CatalogGameView,
  UpdateGameRequest,
} from '../../data-access/platform-catalog-api.service';
import { CreateGameDialog } from '../../components/dialogs/create-game.dialog';

// ── Main Page ────────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-platform-catalog-games-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    ConsoleGamesTableComponent,
    TchPaginationComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TranslatePipe,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-games.page.html',
  styleUrls: ['./platform-catalog-games.page.scss'],
})
export class PlatformCatalogGamesPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  readonly search = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() => numberParam(this.queryParamMap().get('size'), 20));

  readonly games = this.api.listGamesResource(
    () => ({
      q: this.search() || undefined,
      page: this.page(),
      size: this.size(),
    }),
    { suppressShellFeedback: true },
  );
  readonly gamesError = resourceErrorVm(this.games, 'platform.catalog.games');
  readonly gamePage = computed<TchPage<CatalogGameView> | null>(() => {
    const status = this.games.status();
    if (status !== 'resolved' && status !== 'local' && status !== 'reloading') return null;
    return this.games.value() ?? null;
  });
  readonly gamesData = computed(() => this.gamePage()?.items ?? []);
  readonly gameRows = computed<readonly ConsoleGameRow[]>(() => this.gamesData().map(game => this.toConsoleGameRow(game)));
  readonly hasGames = computed(() => this.gamesData().length > 0);
  readonly totalElements = computed(() => this.gamePage()?.totalElements ?? 0);
  readonly pageIndex = computed(() => this.gamePage()?.page ?? this.page());
  readonly pageSize = computed(() => this.gamePage()?.size ?? this.size());
  readonly selectedGame = signal<CatalogGameView | null>(null);
  readonly savingConfig = signal(false);
  readonly categories = ['LOTTO', 'PICK', 'MARRIAGE'] as const;

  readonly configForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    category: [''],
    combination: [''],
    minDigits: [0],
    maxDigits: [0],
    description: [''],
    sortOrder: [10],
    active: [true],
  });

  load(): void {
    this.games.reload();
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
    const ref = this.dialog.open(CreateGameDialog, { width: 'min(100vw - 32px, 520px)' });
    ref.afterClosed().subscribe((created: CatalogGameView | null) => {
      if (created) {
        this.snackBar.open(this.translate.instant('platform.catalog.games.feedback.created'), 'OK', { duration: 4000 });
        this.load();
      }
    });
  }

  openConfig(game: CatalogGameView): void {
    this.selectedGame.set(game);
    this.configForm.reset({
      name: game.name,
      category: game.category ?? '',
      combination: game.combination ?? '',
      minDigits: game.minDigits,
      maxDigits: game.maxDigits,
      description: game.description ?? '',
      sortOrder: game.sortOrder,
      active: game.active,
    });
  }

  closeConfig(): void {
    this.selectedGame.set(null);
  }

  saveConfig(): void {
    const game = this.selectedGame();
    if (!game || this.configForm.invalid || this.savingConfig()) return;

    this.savingConfig.set(true);
    const v = this.configForm.getRawValue();
    const req: UpdateGameRequest = {
      name: v.name.trim(),
      category: v.category || null,
      combination: v.combination || null,
      minDigits: v.minDigits,
      maxDigits: v.maxDigits,
      description: v.description || null,
      sortOrder: v.sortOrder,
      active: v.active,
    };
    this.api.updateGame(game.id, req).subscribe({
      next: updated => {
        this.savingConfig.set(false);
        this.snackBar.open(this.translate.instant('platform.catalog.games.feedback.updated'), 'OK', { duration: 4000 });
        this.openConfig(updated);
        this.load();
      },
      error: (err: unknown) => {
        this.savingConfig.set(false);
        this.snackBar.open(
          (err as { error?: { detail?: string; title?: string } })?.error?.detail
            ?? (err as { error?: { title?: string } })?.error?.title
            ?? this.translate.instant('common.error.title'),
          'OK',
          { duration: 5000 },
        );
      },
    });
  }

  deactivate(game: CatalogGameView): void {
    const data: TchConfirmDialogData = {
      title: this.translate.instant('common.deactivate'),
      message: this.translate.instant('platform.catalog.games.confirm.deactivate', { name: game.name }),
      confirmLabel: this.translate.instant('common.deactivate'),
      destructive: true,
      icon: 'block',
    };
    this.dialog.open(TchConfirmDialog, { data }).afterClosed().subscribe(result => {
      if (!result?.confirmed) return;
      this.api.deactivateGame(game.id).subscribe({
        next: () => {
          this.snackBar.open(this.translate.instant('platform.catalog.games.feedback.deactivated'), 'OK', { duration: 4000 });
          this.load();
        },
        error: (err: unknown) => {
          this.snackBar.open(
            (err as { error?: { title?: string } })?.error?.title ?? this.translate.instant('common.error.title'),
            'OK',
            { duration: 5000 },
          );
        },
      });
    });
  }

  delete(game: CatalogGameView): void {
    const data: TchConfirmDialogData = {
      title: this.translate.instant('common.delete'),
      message: this.translate.instant('platform.catalog.games.confirm.delete', { name: game.name }),
      confirmLabel: this.translate.instant('common.delete'),
      destructive: true,
      icon: 'delete',
    };
    this.dialog.open(TchConfirmDialog, { data }).afterClosed().subscribe(result => {
      if (!result?.confirmed) return;
      this.api.deleteGame(game.id).subscribe({
        next: () => {
          this.snackBar.open(this.translate.instant('platform.catalog.games.feedback.deleted'), 'OK', { duration: 4000 });
          this.load();
        },
        error: (err: unknown) => {
          this.snackBar.open(
            (err as { error?: { title?: string } })?.error?.title ?? this.translate.instant('common.error.title'),
            'OK',
            { duration: 5000 },
          );
        },
      });
    });
  }

  onGameAction(event: ConsoleGameActionEvent): void {
    const game = this.gamesData().find(item => item.id === event.row.id);
    if (!game) return;
    switch (event.action.id) {
      case 'edit':
        this.openConfig(game);
        break;
      case 'deactivate':
        this.deactivate(game);
        break;
      case 'delete':
        this.delete(game);
        break;
    }
  }

  private toConsoleGameRow(game: CatalogGameView): ConsoleGameRow {
    return {
      id: game.id,
      code: game.code,
      name: consoleGameName(game.code, game.name),
      category: game.category,
      sortOrder: game.sortOrder,
      statusLabel: this.translate.instant(game.active ? 'common.enabled' : 'common.disabled'),
      statusTone: game.active ? 'success' : 'neutral',
      actions: [
        { id: 'edit', label: this.translate.instant('common.edit'), icon: 'tune', variant: 'icon' },
        ...(game.active
          ? [{
              id: 'deactivate',
              label: this.translate.instant('common.deactivate'),
              icon: 'block',
              variant: 'icon' as const,
            }]
          : []),
        { id: 'delete', label: this.translate.instant('common.delete'), icon: 'delete', tone: 'danger', variant: 'icon' },
      ],
    };
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
