import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
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
import { PlatformCatalogApi, CatalogThemeView, CreateThemeRequest, UpdateThemeRequest } from '../../data-access/platform-catalog-api.service';

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
  templateUrl: './platform-catalog-themes.page.html',
  styleUrls: ['./platform-catalog-themes.page.scss'],
})
export class PlatformCatalogThemesPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'vendor', 'labelKey', 'active', 'isDefault', 'actions'];
  readonly themes = this.api.listThemesResource({ suppressShellFeedback: true });
  readonly themesError = resourceErrorVm(this.themes, 'platform.catalog.themes');
  readonly themeRows = computed(() => this.themes.value() ?? []);
  readonly selectedTheme = signal<CatalogThemeView | null>(null);
  readonly savingConfig = signal(false);

  readonly configForm = this.fb.nonNullable.group({
    vendor: [''],
    labelKey: [''],
    config: ['{}'],
    active: [true],
  });

  private showError(msg: string): void {
    this.snackBar.open(msg, 'OK', { duration: 5000 });
  }

  load(): void {
    this.themes.reload();
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateThemeDialog, { width: '520px' });
    ref.afterClosed().subscribe((created: CatalogThemeView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Thème créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openConfig(theme: CatalogThemeView): void {
    this.selectedTheme.set(theme);
    this.configForm.reset({
      vendor: theme.vendor ?? '',
      labelKey: theme.labelKey ?? '',
      config: theme.config ? JSON.stringify(theme.config, null, 2) : '{}',
      active: theme.active,
    });
  }

  closeConfig(): void {
    this.selectedTheme.set(null);
  }

  saveConfig(): void {
    const theme = this.selectedTheme();
    if (!theme || this.savingConfig()) return;

    this.savingConfig.set(true);
    const v = this.configForm.getRawValue();
    const req: UpdateThemeRequest = {
      vendor: v.vendor || null,
      labelKey: v.labelKey || null,
      config: v.config || null,
      active: v.active,
    };
    this.api.updateTheme(theme.id, req).subscribe({
      next: updated => {
        this.savingConfig.set(false);
        this.snackBar.open('Thème mis à jour.', 'OK', { duration: 4000 });
        this.openConfig(updated);
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
