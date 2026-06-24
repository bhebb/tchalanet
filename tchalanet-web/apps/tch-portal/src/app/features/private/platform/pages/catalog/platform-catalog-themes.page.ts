import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import { PlatformCatalogApi, CatalogThemeView, CreateThemeRequest, UpdateThemeRequest } from '../../platform-catalog-api.service';

@Component({
  selector: 'tch-create-theme-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatCheckboxModule],
  template: `
    <h2 mat-dialog-title>Nouveau thème</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: DEFAULT_DARK" />
          @if (form.controls.code.invalid && form.controls.code.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Vendeur (vendor)</mat-label>
          <input matInput formControlName="vendor" placeholder="ex: tchalanet" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé de libellé (labelKey)</mat-label>
          <input matInput formControlName="labelKey" placeholder="ex: theme.default_dark" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Configuration JSON</mat-label>
          <textarea matInput formControlName="config" rows="4" placeholder='{"primaryColor":"#000"}'></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Créer</button>
    </mat-dialog-actions>
  `,
})
export class CreateThemeDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreateThemeDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    vendor: [''],
    labelKey: [''],
    config: ['{}'],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreateThemeRequest = {
      code: v.code.toUpperCase(),
      vendor: v.vendor || null,
      labelKey: v.labelKey || null,
      config: v.config || null,
      active: v.active,
    };
    this.api.createTheme(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-edit-theme-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatCheckboxModule],
  template: `
    <h2 mat-dialog-title>Modifier — {{ theme().code }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Vendeur (vendor)</mat-label>
          <input matInput formControlName="vendor" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé de libellé (labelKey)</mat-label>
          <input matInput formControlName="labelKey" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Configuration JSON</mat-label>
          <textarea matInput formControlName="config" rows="4"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="saving()" (click)="save()">Enregistrer</button>
    </mat-dialog-actions>
  `,
})
export class EditThemeDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<EditThemeDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly theme = signal<CatalogThemeView>({} as CatalogThemeView);
  readonly form = this.fb.nonNullable.group({
    vendor: [''],
    labelKey: [''],
    config: ['{}'],
    active: [true],
  });

  init(t: CatalogThemeView): void {
    this.theme.set(t);
    this.form.patchValue({
      vendor: t.vendor ?? '',
      labelKey: t.labelKey ?? '',
      config: t.config ? JSON.stringify(t.config, null, 2) : '{}',
      active: t.active,
    });
  }

  save(): void {
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: UpdateThemeRequest = {
      vendor: v.vendor || null,
      labelKey: v.labelKey || null,
      config: v.config || null,
      active: v.active,
    };
    this.api.updateTheme(this.theme().id, req).subscribe({
      next: updated => this.ref.close(updated),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

@Component({
  selector: 'tch-platform-catalog-themes-page',
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
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-themes.page.html',
})
export class PlatformCatalogThemesPage implements OnInit {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'vendor', 'labelKey', 'active', 'isDefault', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly themes = signal<CatalogThemeView[]>([]);

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listThemes().subscribe({
      next: list => { this.themes.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateThemeDialog, { width: '520px' });
    ref.afterClosed().subscribe((created: CatalogThemeView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Thème créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(theme: CatalogThemeView): void {
    const ref = this.dialog.open(EditThemeDialog, { width: '520px' });
    (ref.componentInstance as EditThemeDialog).init(theme);
    ref.afterClosed().subscribe((updated: CatalogThemeView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open('Thème mis à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  delete(theme: CatalogThemeView): void {
    if (!confirm(`Supprimer le thème « ${theme.code} » ?`)) return;
    this.api.deleteTheme(theme.id).subscribe({
      next: () => { this.snackBar.open('Thème supprimé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }
}
