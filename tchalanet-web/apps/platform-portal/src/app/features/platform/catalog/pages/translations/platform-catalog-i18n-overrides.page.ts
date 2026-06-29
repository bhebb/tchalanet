import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  I18nGlobalOverviewView,
  I18nOverrideLevel,
  I18nOverrideView,
  PlatformI18nApi,
} from '../../data-access/platform-i18n-api.service';
import { CreateI18nOverrideDialog, COMMON_LOCALES, LEVELS } from '../../components/dialogs/create-i18n-override.dialog';
import { EditI18nOverrideDialog } from '../../components/dialogs/edit-i18n-override.dialog';

@Component({
  selector: 'tch-platform-catalog-i18n-overrides-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    ReactiveFormsModule,
    TranslatePipe,
  ],
  templateUrl: './platform-catalog-i18n-overrides.page.html',
  styleUrls: ['./platform-catalog-i18n-overrides.page.scss'],
})
export class PlatformCatalogI18nOverridesPage implements OnInit {
  private readonly api = inject(PlatformI18nApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly route = inject(ActivatedRoute);

  readonly displayedColumns = ['locale', 'level', 'i18nKey', 'i18nValue', 'surface', 'active', 'actions'];
  readonly locales = COMMON_LOCALES;
  readonly levels = LEVELS;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly overrides = signal<I18nOverrideView[]>([]);
  readonly overview = signal<I18nGlobalOverviewView | null>(null);
  readonly keyFilter = signal('');
  readonly localeFilter = signal('');
  readonly levelFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

  ngOnInit(): void {
    this.keyFilter.set(this.route.snapshot.queryParamMap.get('key') ?? '');
    this.load();
    this.api.getOverview().subscribe({ next: o => this.overview.set(o) });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listOverrides({
      locale: this.localeFilter() || undefined,
      level: (this.levelFilter() as I18nOverrideLevel) || undefined,
      i18nKeyContains: this.keyFilter() || undefined,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.overrides.set(p.items);
        this.totalElements.set(p.totalElements);
        this.page.set(p.page);
        this.totalPages.set(p.totalPages || 1);
        this.hasNext.set(p.hasNext ?? false);
        this.hasPrevious.set(p.hasPrevious ?? false);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(
          (err as { error?: { title?: string } })?.error?.title ??
            this.translate.instant('platform.i18nOverrides.feedback.loadError'),
        );
        this.loading.set(false);
      },
    });
  }

  onKeyFilter(v: string): void { this.keyFilter.set(v); this.page.set(0); this.load(); }
  onLocaleChange(v: string): void { this.localeFilter.set(v); this.page.set(0); this.load(); }
  onLevelChange(v: string): void { this.levelFilter.set(v); this.page.set(0); this.load(); }
  prevPage(): void { if (this.hasPrevious()) { this.page.set(this.page() - 1); this.load(); } }
  nextPage(): void { if (this.hasNext()) { this.page.set(this.page() + 1); this.load(); } }

  openCreate(): void {
    const ref = this.dialog.open(CreateI18nOverrideDialog, { width: '540px' });
    ref.afterClosed().subscribe((created: I18nOverrideView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open(this.translate.instant('platform.i18nOverrides.feedback.created'), 'OK', { duration: 4000 }); this.load(); this.api.getOverview().subscribe({ next: o => this.overview.set(o) }); }
    });
  }

  openEdit(override: I18nOverrideView): void {
    const ref = this.dialog.open(EditI18nOverrideDialog, { data: override, width: '520px' });
    ref.afterClosed().subscribe((updated: I18nOverrideView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open(this.translate.instant('platform.i18nOverrides.feedback.updated'), 'OK', { duration: 4000 }); this.load(); }
    });
  }

  deleteOverride(override: I18nOverrideView): void {
    if (!confirm(this.translate.instant('platform.i18nOverrides.action.confirmDelete', { key: override.i18nKey, locale: override.locale }))) return;
    this.api.deleteOverride(override.id).subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('platform.i18nOverrides.feedback.deleted'), 'OK', { duration: 4000 });
        this.load();
        this.api.getOverview().subscribe({ next: o => this.overview.set(o) });
      },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? this.translate.instant('platform.i18nOverrides.feedback.deleteError'), 'OK', { duration: 5000 });
      },
    });
  }
}
