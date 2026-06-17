import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  I18nGlobalOverviewView,
  I18nOverrideLevel,
  I18nOverrideView,
  PlatformI18nApi,
} from '../../platform-i18n-api.service';
import { CreateI18nOverrideDialog, COMMON_LOCALES, LEVELS } from './dialogs/create-i18n-override.dialog';
import { EditI18nOverrideDialog } from './dialogs/edit-i18n-override.dialog';

@Component({
  selector: 'tch-platform-ops-i18n-page',
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
  ],
  templateUrl: './platform-ops-i18n.page.html',
  styleUrls: ['./platform-ops-i18n.page.scss'],
})
export class PlatformOpsI18nPage implements OnInit {
  private readonly api = inject(PlatformI18nApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

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

  ngOnInit(): void {
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
        this.overrides.set(p.content);
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onKeyFilter(v: string): void { this.keyFilter.set(v); this.page.set(0); this.load(); }
  onLocaleChange(v: string): void { this.localeFilter.set(v); this.page.set(0); this.load(); }
  onLevelChange(v: string): void { this.levelFilter.set(v); this.page.set(0); this.load(); }
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  openCreate(): void {
    const ref = this.dialog.open(CreateI18nOverrideDialog, { width: '540px' });
    ref.afterClosed().subscribe((created: I18nOverrideView | null) => {
      if (created) {
        this.snackBar.open('Traduction créée.', 'OK', { duration: 4000 });
        this.load();
        this.api.getOverview().subscribe({ next: o => this.overview.set(o) });
      }
    });
  }

  openEdit(override: I18nOverrideView): void {
    const ref = this.dialog.open(EditI18nOverrideDialog, { data: override, width: '520px' });
    ref.afterClosed().subscribe((updated: I18nOverrideView | null) => {
      if (updated) { this.snackBar.open('Traduction mise à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  deleteOverride(override: I18nOverrideView): void {
    this.api.deleteOverride(override.id.value).subscribe({
      next: () => {
        this.snackBar.open('Traduction supprimée.', 'OK', { duration: 4000 });
        this.load();
        this.api.getOverview().subscribe({ next: o => this.overview.set(o) });
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
      },
    });
  }
}
