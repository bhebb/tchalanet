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
import {
  PlatformCatalogApi,
  CatalogDrawChannelView,
  CreateDrawChannelRequest,
  UpdateDrawChannelRequest,
} from '../../platform-catalog-api.service';

// ── Create Dialog ────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-create-draw-channel-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
  ],
  template: `
    <h2 mat-dialog-title>Nouveau canal de tirage</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: TX_MORNING" />
          @if (form.controls.code.invalid && form.controls.code.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.invalid && form.controls.name.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Libellé</mat-label>
          <input matInput formControlName="label" placeholder="ex: Matin" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Heure de tirage (HH:mm)</mat-label>
          <input matInput formControlName="drawTime" placeholder="10:00" />
          @if (form.controls.drawTime.invalid && form.controls.drawTime.touched) {
            <mat-error>Format HH:mm requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Timezone</mat-label>
          <input matInput formControlName="timezone" placeholder="America/Port-au-Prince" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours (ex: MONDAY,TUESDAY)</mat-label>
          <input matInput formControlName="daysOfWeek" placeholder="MONDAY,WEDNESDAY,FRIDAY" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Délai cutoff (secondes)</mat-label>
          <input matInput type="number" formControlName="cutoffSec" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="sortOrder" />
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid || saving()"
        (click)="save()"
      >
        Créer
      </button>
    </mat-dialog-actions>
  `,
})
export class CreateDrawChannelDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreateDrawChannelDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    label: [''],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    timezone: ['America/Port-au-Prince'],
    daysOfWeek: [''],
    cutoffSec: [null as number | null],
    sortOrder: [10],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const days = v.daysOfWeek
      ? v.daysOfWeek
          .split(',')
          .map(d => d.trim().toUpperCase())
          .filter(Boolean)
      : null;
    const req: CreateDrawChannelRequest = {
      code: v.code.toUpperCase(),
      name: v.name,
      label: v.label || null,
      drawTime: v.drawTime,
      timezone: v.timezone || null,
      daysOfWeek: days?.length ? days : null,
      cutoffSec: v.cutoffSec ?? null,
      sortOrder: v.sortOrder,
      active: v.active,
    };
    this.api.createDrawChannel(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Edit Dialog ──────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-edit-draw-channel-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
  ],
  template: `
    <h2 mat-dialog-title>Modifier — {{ channel().name }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Libellé</mat-label>
          <input matInput formControlName="label" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Heure de tirage (HH:mm)</mat-label>
          <input matInput formControlName="drawTime" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Timezone</mat-label>
          <input matInput formControlName="timezone" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours (ex: MONDAY,TUESDAY)</mat-label>
          <input matInput formControlName="daysOfWeek" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Délai cutoff (secondes)</mat-label>
          <input matInput type="number" formControlName="cutoffSec" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Ordre d'affichage</mat-label>
          <input matInput type="number" formControlName="sortOrder" />
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid || saving()"
        (click)="save()"
      >
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
})
export class EditDrawChannelDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<EditDrawChannelDialog>);
  private readonly fb = inject(FormBuilder);

  readonly channel = signal<CatalogDrawChannelView>({} as CatalogDrawChannelView);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    label: [''],
    drawTime: ['', [Validators.required, Validators.pattern(/^\d{2}:\d{2}/)]],
    timezone: [''],
    daysOfWeek: [''],
    cutoffSec: [null as number | null],
    sortOrder: [10],
    active: [true],
  });

  init(ch: CatalogDrawChannelView): void {
    this.channel.set(ch);
    this.form.patchValue({
      code: ch.code,
      name: ch.name,
      label: ch.label ?? '',
      drawTime: ch.drawTime?.substring(0, 5) ?? '',
      timezone: ch.timezone ?? '',
      daysOfWeek: ch.daysOfWeek?.join(',') ?? '',
      cutoffSec: ch.cutoffSec ?? null,
      sortOrder: ch.sortOrder,
      active: ch.active,
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
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
      active: v.active,
    };
    this.api.updateDrawChannel(this.channel().id, req).subscribe({
      next: updated => this.ref.close(updated),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

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
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-draw-channels.page.html',
})
export class PlatformCatalogDrawChannelsPage implements OnInit {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = [
    'code',
    'name',
    'drawTime',
    'timezone',
    'daysOfWeek',
    'sortOrder',
    'active',
    'actions',
  ];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly channels = signal<CatalogDrawChannelView[]>([]);
  readonly search = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .listDrawChannels({ q: this.search() || undefined, page: this.page(), size: 20 })
      .subscribe({
        next: p => {
          this.channels.set(p.items);
          this.totalElements.set(p.totalElements);
          this.totalPages.set(p.totalPages || 1);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          this.error.set(
            (err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.',
          );
          this.loading.set(false);
        },
      });
  }

  onSearch(v: string): void {
    this.search.set(v);
    this.page.set(0);
    this.load();
  }
  prevPage(): void {
    this.page.set(this.page() - 1);
    this.load();
  }
  nextPage(): void {
    this.page.set(this.page() + 1);
    this.load();
  }

  openCreate(): void {
    const ref = this.dialog.open(CreateDrawChannelDialog, { width: '520px' });
    ref.afterClosed().subscribe((created: CatalogDrawChannelView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Canal créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(ch: CatalogDrawChannelView): void {
    const ref = this.dialog.open(EditDrawChannelDialog, { width: '520px' });
    (ref.componentInstance as EditDrawChannelDialog).init(ch);
    ref.afterClosed().subscribe((updated: CatalogDrawChannelView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open('Canal mis à jour.', 'OK', { duration: 4000 }); this.load(); }
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
}
