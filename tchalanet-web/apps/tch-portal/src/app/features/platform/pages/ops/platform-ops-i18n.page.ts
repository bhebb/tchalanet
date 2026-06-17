import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
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
import {
  I18nGlobalOverviewView,
  I18nOverrideView,
  PlatformI18nApi,
} from '../../platform-i18n-api.service';

const COMMON_LOCALES = ['fr', 'en', 'ht', 'es'];

// ── Create Override Dialog ─────────────────────────────────────────────────────

@Component({
  selector: 'tch-create-i18n-override-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Ajouter une traduction</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Locale</mat-label>
          <mat-select formControlName="locale">
            @for (loc of locales; track loc) {
              <mat-option [value]="loc">{{ loc }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Clé</mat-label>
          <input matInput formControlName="key" />
          @if (form.controls['key'].invalid && form.controls['key'].touched) {
            <mat-error>Clé requise.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="value" rows="4"></textarea>
          @if (form.controls['value'].invalid && form.controls['value'].touched) {
            <mat-error>Valeur requise.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Créer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 400px; }
      .full-width { width: 100%; }
      .error-panel {
        display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem;
        border-radius: 0.5rem; background: var(--tch-color-error-container);
        color: var(--tch-color-on-error-container); font-size: 0.875rem; margin-top: 0.5rem;
      }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class CreateI18nOverrideDialog {
  private readonly dialogRef = inject(MatDialogRef<CreateI18nOverrideDialog>);
  private readonly api = inject(PlatformI18nApi);
  private readonly fb = inject(FormBuilder);

  readonly locales = COMMON_LOCALES;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    locale: ['fr', Validators.required],
    key: ['', Validators.required],
    value: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    const v = this.form.value as { locale: string; key: string; value: string };
    this.api.createOverride(v).subscribe({
      next: created => { this.submitting.set(false); this.dialogRef.close(created); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la création.');
      },
    });
  }
}

// ── Edit Override Dialog ───────────────────────────────────────────────────────

@Component({
  selector: 'tch-edit-i18n-override-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Modifier la traduction</h2>
    <mat-dialog-content>
      <div class="readonly-info">
        <span class="readonly-label">Locale</span><span>{{ data.locale }}</span>
        <span class="readonly-label">Clé</span><span class="mono">{{ data.key }}</span>
      </div>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="value" rows="4"></textarea>
          @if (form.controls['value'].invalid && form.controls['value'].touched) {
            <mat-error>Valeur requise.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .readonly-info {
        display: grid; grid-template-columns: auto 1fr; gap: 0.25rem 0.75rem;
        margin-bottom: 1rem; font-size: 0.875rem;
      }
      .readonly-label { color: var(--tch-color-on-surface-variant); font-weight: 500; }
      .mono { font-family: monospace; }
      .dialog-form { min-width: 400px; }
      .full-width { width: 100%; }
      .error-panel {
        display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem;
        border-radius: 0.5rem; background: var(--tch-color-error-container);
        color: var(--tch-color-on-error-container); font-size: 0.875rem; margin-top: 0.5rem;
      }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class EditI18nOverrideDialog {
  protected readonly data = inject<I18nOverrideView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditI18nOverrideDialog>);
  private readonly api = inject(PlatformI18nApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    value: [this.data.value, Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateOverride(this.data.id.value, { value: this.form.value.value! }).subscribe({
      next: updated => { this.submitting.set(false); this.dialogRef.close(updated); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la mise à jour.');
      },
    });
  }
}

// ── Main Page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-i18n-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
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
  template: `
    <tch-admin-page-shell title="Traductions (overrides i18n)" description="Gérez les overrides de traduction par locale.">
      <div actions>
        <button mat-flat-button color="primary" (click)="openCreate()">
          <span class="material-symbols-outlined">add</span>
          Ajouter
        </button>
      </div>

      @if (overview()) {
        <div class="kpi-row">
          <div class="kpi-chip">
            <span class="kpi-value">{{ overview()!.summary.totalKeys }}</span>
            <span class="kpi-label">Clés</span>
          </div>
          <div class="kpi-chip">
            <span class="kpi-value">{{ overview()!.summary.totalLocales }}</span>
            <span class="kpi-label">Locales</span>
          </div>
          <div class="kpi-chip">
            <span class="kpi-value">{{ overview()!.summary.totalOverrides }}</span>
            <span class="kpi-label">Overrides</span>
          </div>
        </div>
      }

      <tch-admin-crud-shell>
        <div toolbar>
          <tch-admin-data-toolbar
            searchPlaceholder="Rechercher par clé..."
            [searchValue]="search()"
            (searchChange)="onSearch($event)"
          >
            <mat-form-field appearance="outline" style="min-width:130px">
              <mat-label>Locale</mat-label>
              <mat-select [value]="localeFilter()" (valueChange)="onLocaleChange($event)">
                <mat-option value="">Toutes</mat-option>
                @for (loc of locales; track loc) {
                  <mat-option [value]="loc">{{ loc }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <button mat-stroked-button (click)="load()">
              <span class="material-symbols-outlined">refresh</span>
            </button>
          </tch-admin-data-toolbar>
        </div>

        <div content>
          @if (loading()) {
            <tch-loading label="Chargement..." />
          } @else if (error()) {
            <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
          } @else if (overrides().length === 0) {
            <tch-admin-empty-state icon="translate" title="Aucun override" message="Aucune traduction surchargée." />
          } @else {
            <div class="table-container">
              <table mat-table [dataSource]="overrides()">
                <ng-container matColumnDef="locale">
                  <th mat-header-cell *matHeaderCellDef>Locale</th>
                  <td mat-cell *matCellDef="let row">{{ row.locale }}</td>
                </ng-container>
                <ng-container matColumnDef="key">
                  <th mat-header-cell *matHeaderCellDef>Clé</th>
                  <td mat-cell *matCellDef="let row"><span style="font-family:monospace">{{ row.key }}</span></td>
                </ng-container>
                <ng-container matColumnDef="value">
                  <th mat-header-cell *matHeaderCellDef>Valeur</th>
                  <td mat-cell *matCellDef="let row">{{ row.value.length > 60 ? row.value.slice(0, 60) + '…' : row.value }}</td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <div class="action-row">
                      <button mat-icon-button (click)="openEdit(row)" title="Modifier">
                        <span class="material-symbols-outlined">edit</span>
                      </button>
                      <button mat-icon-button color="warn" (click)="deleteOverride(row)" title="Supprimer">
                        <span class="material-symbols-outlined">delete</span>
                      </button>
                    </div>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
          }
        </div>

        <div footer>
          <span class="footer-count">{{ totalElements() }} override(s)</span>
          <div class="pagination">
            <button mat-icon-button [disabled]="page() === 0" (click)="prevPage()">
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
            <button mat-icon-button [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </tch-admin-crud-shell>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .kpi-row { display: flex; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
      .kpi-chip {
        display: flex; flex-direction: column; align-items: center; padding: 0.75rem 1.25rem;
        border: 1px solid var(--tch-color-outline-variant); border-radius: 0.75rem;
        background: var(--tch-color-surface-container-low);
      }
      .kpi-value { font-size: 1.5rem; font-weight: 700; color: var(--tch-color-primary); }
      .kpi-label { font-size: 0.75rem; color: var(--tch-color-on-surface-variant); }
      .table-container { overflow-x: auto; }
      table { width: 100%; }
      .action-row { display: flex; gap: 0.25rem; }
      .footer-count { font-size: 0.875rem; color: var(--tch-color-on-surface-variant); }
      .pagination { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; }
    `,
  ],
})
export class PlatformOpsI18nPage implements OnInit {
  private readonly api = inject(PlatformI18nApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['locale', 'key', 'value', 'actions'];
  readonly locales = COMMON_LOCALES;
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly overrides = signal<I18nOverrideView[]>([]);
  readonly overview = signal<I18nGlobalOverviewView | null>(null);
  readonly search = signal('');
  readonly localeFilter = signal('');
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
    this.api.listOverrides({ locale: this.localeFilter() || undefined, q: this.search() || undefined, page: this.page(), size: 20 }).subscribe({
      next: page => {
        this.overrides.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onSearch(v: string): void { this.search.set(v); this.page.set(0); this.load(); }
  onLocaleChange(v: string): void { this.localeFilter.set(v); this.page.set(0); this.load(); }
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  openCreate(): void {
    const ref = this.dialog.open(CreateI18nOverrideDialog, { width: '500px' });
    ref.afterClosed().subscribe((created: I18nOverrideView | null) => {
      if (created) { this.snackBar.open('Traduction créée.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(override: I18nOverrideView): void {
    const ref = this.dialog.open(EditI18nOverrideDialog, { data: override, width: '500px' });
    ref.afterClosed().subscribe((updated: I18nOverrideView | null) => {
      if (updated) { this.snackBar.open('Traduction mise à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  deleteOverride(override: I18nOverrideView): void {
    this.api.deleteOverride(override.id.value).subscribe({
      next: () => { this.snackBar.open('Traduction supprimée.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
      },
    });
  }
}
