import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { PlatformOpsApi, CacheView } from '../../platform-ops-api.service';

// ── Clear All Caches dialog ────────────────────────────────────────────────────

@Component({
  selector: 'tch-clear-all-caches-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Vider tous les caches</h2>
    <mat-dialog-content>
      <div class="audit-warning">
        <span class="material-symbols-outlined">warning</span>
        Cette action vide tous les caches de l'application. Elle peut provoquer une dégradation
        temporaire des performances.
      </div>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Raison (min. 10 caractères)</mat-label>
          <textarea matInput formControlName="reason" rows="3"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Raison requise (min. 10 caractères).</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="confirmed">
          Je confirme vouloir vider tous les caches.
        </mat-checkbox>
      </form>

      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
          @if (traceId()) {
            <span class="trace-id">ID: {{ traceId() }}</span>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button
        mat-flat-button
        color="warn"
        [disabled]="form.invalid || submitting()"
        (click)="submit()"
      >
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Vider tout
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .audit-warning {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem;
        border-radius: 0.5rem;
        background: var(--tch-color-warning-container, #fff3cd);
        color: var(--tch-color-on-warning-container, #92400e);
        font-size: 0.875rem;
        margin-bottom: 1rem;
      }
      .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 400px; }
      .full-width { width: 100%; }
      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
        font-size: 0.875rem;
        margin-top: 0.5rem;
      }
      .trace-id { font-size: 0.75rem; opacity: 0.7; margin-left: 0.25rem; }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class ClearAllCachesDialog {
  private readonly dialogRef = inject(MatDialogRef<ClearAllCachesDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);

  readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
    confirmed: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);

    this.api.clearAllCaches(this.form.controls.reason.value!).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Tous les caches ont été vidés.', 'OK', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de l\'opération.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
      },
    });
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-cache-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell title="Gestion des caches" description="Visualisez et videz les caches applicatifs.">
      <div actions>
        <button mat-flat-button color="warn" (click)="openClearAll()">
          <span class="material-symbols-outlined">delete_sweep</span>
          Vider tous les caches
        </button>
      </div>

      @if (loading()) {
        <tch-loading label="Chargement..." />
      } @else if (error()) {
        <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
      } @else if (caches().length === 0) {
        <tch-admin-empty-state icon="storage" title="Aucun cache" message="Aucun cache trouvé." />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="caches()">
            <ng-container matColumnDef="cacheName">
              <th mat-header-cell *matHeaderCellDef>Cache</th>
              <td mat-cell *matCellDef="let row">{{ row.cacheName }}</td>
            </ng-container>
            <ng-container matColumnDef="size">
              <th mat-header-cell *matHeaderCellDef>Taille</th>
              <td mat-cell *matCellDef="let row">{{ row.size }}</td>
            </ng-container>
            <ng-container matColumnDef="hitRate">
              <th mat-header-cell *matHeaderCellDef>Taux de succès</th>
              <td mat-cell *matCellDef="let row">
                {{ row.hitRate !== undefined ? (row.hitRate * 100 | number: '1.1-1') + '%' : '—' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="lastClearedAt">
              <th mat-header-cell *matHeaderCellDef>Dernier vidage</th>
              <td mat-cell *matCellDef="let row">{{ row.lastClearedAt ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let row">
                <button mat-stroked-button color="warn" (click)="clearOne(row)">
                  <span class="material-symbols-outlined">delete</span>
                  Vider
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>
        </div>
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .table-container { overflow-x: auto; }
      table { width: 100%; }
    `,
  ],
})
export class PlatformOpsCachePage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['cacheName', 'size', 'hitRate', 'lastClearedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly caches = signal<CacheView[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listCaches().subscribe({
      next: v => { this.caches.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });
  }

  clearOne(cache: CacheView): void {
    this.api.clearCache(cache.cacheName).subscribe({
      next: () => {
        this.snackBar.open(`Cache "${cache.cacheName}" vidé.`, 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        const msg = pd?.title ?? 'Erreur.';
        const tid = pd?.errorId ?? pd?.requestId;
        this.snackBar.open(tid ? `${msg} (ID: ${tid})` : msg, 'OK', { duration: 5000 });
      },
    });
  }

  openClearAll(): void {
    const ref = this.dialog.open(ClearAllCachesDialog, { width: '480px' });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.load();
    });
  }
}
