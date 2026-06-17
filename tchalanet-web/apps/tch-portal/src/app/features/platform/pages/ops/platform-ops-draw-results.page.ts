import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { DatePipe } from '@angular/common';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  DrawResultOpsResponse,
  FetchExternalResultsRequest,
  FetchExternalResultsWindowResult,
  RefreshExternalResultsWindowResult,
  OverrideDrawResultRequest,
  RecordManualDrawResultRequest,
} from '../../platform-ops-api.service';

// ── Fetch/Refresh dialog ──────────────────────────────────────────────────────

@Component({
  selector: 'tch-fetch-results-dialog',
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
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Date de base (YYYY-MM-DD, optionnel)</mat-label>
          <input matInput formControlName="baseDate" placeholder="2026-06-17" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours en arrière</mat-label>
          <input matInput type="number" formControlName="daysBack" min="0" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Slot keys (optionnel, virgule-séparé)</mat-label>
          <input matInput formControlName="slotKeys" placeholder="NY_MID, FL_EVE" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Max slots</mat-label>
          <input matInput type="number" formControlName="maxSlots" min="1" />
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer l'écrasement</mat-checkbox>
        @if (form.controls.force.value) {
          <mat-form-field appearance="outline">
            <mat-label>Raison (requis si force=true)</mat-label>
            <input matInput formControlName="reason" />
            @if (form.controls.reason.invalid && form.controls.reason.touched) {
              <mat-error>Raison requise.</mat-error>
            }
          </mat-form-field>
        }
        <mat-checkbox formControlName="dryRun">Dry-run (simuler sans écrire)</mat-checkbox>
        <mat-checkbox formControlName="includeRaw">Persister le payload brut</mat-checkbox>
      </form>

      @if (result()) {
        <div class="result-panel">
          @if (data.mode === 'fetch') {
            <p>Insérés: {{ fetchResult()!.inserted }} · Mis à jour: {{ fetchResult()!.updated }} · Ignorés: {{ fetchResult()!.skipped }} · Non trouvés: {{ fetchResult()!.notFound }}</p>
          } @else {
            <p>Récupérés: {{ refreshResult()!.fetched }} · Upserted: {{ refreshResult()!.upserted }} · Appliqués: {{ refreshResult()!.applied }} · Non trouvés: {{ refreshResult()!.notFound }} · Erreurs: {{ refreshResult()!.projectedFail }}</p>
          }
        </div>
      }
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Exécuter
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 420px; }
    .result-panel { background: var(--tch-color-success-container, #d4edda); color: var(--tch-color-on-success-container, #155724); padding: 0.75rem; border-radius: 0.5rem; margin-top: 0.75rem; font-size: 0.875rem; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; margin-top: 0.5rem; font-size: 0.875rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class FetchResultsDialog {
  protected readonly data = inject<{ title: string; mode: 'fetch' | 'refresh'; onSuccess: () => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<FetchResultsDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<FetchExternalResultsWindowResult | RefreshExternalResultsWindowResult | null>(null);
  readonly fetchResult = computed(() => this.data.mode === 'fetch' ? this.result() as FetchExternalResultsWindowResult : null);
  readonly refreshResult = computed(() => this.data.mode === 'refresh' ? this.result() as RefreshExternalResultsWindowResult : null);

  readonly form = this.fb.group({
    baseDate: [''],
    daysBack: [0],
    slotKeys: [''],
    maxSlots: [200],
    force: [false],
    reason: [''],
    dryRun: [false],
    includeRaw: [false],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    if (v.force && !v.reason) {
      this.form.controls.reason.setValidators(Validators.required);
      this.form.controls.reason.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }

    const req: FetchExternalResultsRequest = {
      baseDate: v.baseDate || undefined,
      daysBack: v.daysBack ?? 0,
      slotKeys: v.slotKeys ? v.slotKeys.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      force: v.force ?? false,
      dryRun: v.dryRun ?? false,
      maxSlots: v.maxSlots ?? 200,
      reason: v.reason || undefined,
      includeRaw: v.includeRaw ?? false,
    };

    this.submitting.set(true);
    this.error.set(null);

    const onNext = (res: FetchExternalResultsWindowResult | RefreshExternalResultsWindowResult) => {
      this.submitting.set(false);
      this.result.set(res);
      this.data.onSuccess();
    };
    const onError = (err: unknown) => {
      this.submitting.set(false);
      const pd = (err as { error?: { title?: string } })?.error;
      this.error.set(pd?.title ?? 'Erreur.');
    };

    if (this.data.mode === 'fetch') {
      this.api.fetchDrawResults(req).subscribe({ next: onNext, error: onError });
    } else {
      this.api.refreshDrawResults(req).subscribe({ next: onNext, error: onError });
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}

// ── Override dialog ───────────────────────────────────────────────────────────

@Component({
  selector: 'tch-override-result-dialog',
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
    <h2 mat-dialog-title>Override — {{ data.row.slotKey }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Tenant ID</mat-label>
          <input matInput formControlName="tenantId" />
          @if (form.controls.tenantId.invalid && form.controls.tenantId.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date du tirage (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="drawDate" />
          @if (form.controls.drawDate.invalid && form.controls.drawDate.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick 3 (ex: 1-2-3)</mat-label>
          <input matInput formControlName="pick3" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick 4 (ex: 1-2-3-4)</mat-label>
          <input matInput formControlName="pick4" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer (remplace états protégés)</mat-checkbox>
      </form>
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="warn" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Appliquer l'override
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 420px; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class OverrideResultDialog {
  protected readonly data = inject<{ row: DrawResultOpsResponse; onSuccess: () => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OverrideResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    tenantId: ['', Validators.required],
    drawDate: ['', Validators.required],
    pick3: [''],
    pick4: [''],
    reason: ['', Validators.required],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: OverrideDrawResultRequest = {
      tenantId: v.tenantId!,
      slotKey: this.data.row.slotKey,
      drawDate: v.drawDate!,
      pick3: v.pick3 || undefined,
      pick4: v.pick4 || undefined,
      reason: v.reason!,
      force: v.force ?? false,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.overrideDrawResult(req).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Override appliqué.', 'OK', { duration: 3000 });
        this.data.onSuccess();
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur.');
      },
    });
  }
}

// ── Main page ──────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Résultats des tirages"
      description="Consultation, fetch, override et confirmation des résultats."
    >
      <div actions>
        <button mat-stroked-button [disabled]="actionLoading()" (click)="openFetch('fetch')">
          <span class="material-symbols-outlined">download</span>
          Fetch
        </button>
        <button mat-stroked-button [disabled]="actionLoading()" (click)="openFetch('refresh')">
          <span class="material-symbols-outlined">refresh</span>
          Refresh
        </button>
      </div>

      @if (loading()) {
        <tch-loading label="Chargement..." />
      } @else if (error()) {
        <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
      } @else if (page()?.content?.length === 0) {
        <tch-admin-empty-state
          icon="receipt_long"
          title="Aucun résultat"
          message="Aucun résultat de tirage disponible."
        />
      } @else {
        <tch-admin-crud-shell>
          <ng-container content>
            <div class="table-wrap">
              <table mat-table [dataSource]="page()?.content ?? []">
                <ng-container matColumnDef="slotKey">
                  <th mat-header-cell *matHeaderCellDef>Slot</th>
                  <td mat-cell *matCellDef="let row"><code>{{ row.slotKey }}</code></td>
                </ng-container>
                <ng-container matColumnDef="occurredAt">
                  <th mat-header-cell *matHeaderCellDef>Date</th>
                  <td mat-cell *matCellDef="let row">{{ row.occurredAt | date: 'yyyy-MM-dd HH:mm' }}</td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Statut</th>
                  <td mat-cell *matCellDef="let row">
                    <tch-admin-status-pill [tone]="statusTone(row.status)" [label]="row.status" />
                  </td>
                </ng-container>
                <ng-container matColumnDef="source">
                  <th mat-header-cell *matHeaderCellDef>Source</th>
                  <td mat-cell *matCellDef="let row">{{ row.source }}</td>
                </ng-container>
                <ng-container matColumnDef="quality">
                  <th mat-header-cell *matHeaderCellDef>Qualité</th>
                  <td mat-cell *matCellDef="let row">
                    <tch-admin-status-pill [tone]="qualityTone(row.quality)" [label]="row.quality" />
                  </td>
                </ng-container>
                <ng-container matColumnDef="fetchedAt">
                  <th mat-header-cell *matHeaderCellDef>Récupéré le</th>
                  <td mat-cell *matCellDef="let row">{{ row.fetchedAt ? (row.fetchedAt | date: 'MM-dd HH:mm') : '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <div class="row-actions">
                      @if (row.status === 'PROVISIONAL') {
                        <button mat-stroked-button (click)="confirmResult(row)">
                          <span class="material-symbols-outlined">check_circle</span>
                          Confirmer
                        </button>
                      }
                      <button mat-icon-button title="Override" (click)="openOverride(row)">
                        <span class="material-symbols-outlined">edit</span>
                      </button>
                    </div>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
          </ng-container>
          <ng-container footer>
            <mat-paginator
              [length]="page()?.totalElements ?? 0"
              [pageSize]="pageSize()"
              [pageIndex]="pageIndex()"
              [pageSizeOptions]="[20, 50, 100]"
              (page)="onPage($event)"
            />
          </ng-container>
        </tch-admin-crud-shell>
      }
    </tch-admin-page-shell>
  `,
  styles: [`
    .table-wrap { overflow-x: auto; }
    table { width: 100%; }
    code { font-family: monospace; font-size: 0.8125rem; }
    .row-actions { display: flex; gap: 0.25rem; align-items: center; }
  `],
})
export class PlatformOpsDrawResultsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['slotKey', 'occurredAt', 'status', 'source', 'quality', 'fetchedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly page = signal<{ content: DrawResultOpsResponse[]; totalElements: number; totalPages: number; number: number; size: number } | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly actionLoading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listDrawResults({ page: this.pageIndex(), size: this.pageSize(), sort: 'occurredAt,DESC' }).subscribe({
      next: v => { this.page.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.load();
  }

  openFetch(mode: 'fetch' | 'refresh'): void {
    this.dialog.open(FetchResultsDialog, {
      data: {
        title: mode === 'fetch' ? 'Fetch résultats externes' : 'Refresh résultats (fetch + apply)',
        mode,
        onSuccess: () => this.load(),
      },
      width: '500px',
    });
  }

  openOverride(row: DrawResultOpsResponse): void {
    this.dialog.open(OverrideResultDialog, {
      data: { row, onSuccess: () => this.load() },
      width: '500px',
    });
  }

  confirmResult(row: DrawResultOpsResponse): void {
    this.actionLoading.set(true);
    this.api.confirmDrawResult(row.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Résultat confirmé.', 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        this.actionLoading.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur de confirmation.', 'OK', { duration: 5000 });
      },
    });
  }

  statusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral' | 'info'> = {
      CONFIRMED: 'success',
      PROVISIONAL: 'warning',
      OVERRIDDEN: 'info',
      MANUAL: 'info',
      REJECTED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  qualityTone(quality: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
      HIGH: 'success',
      MEDIUM: 'warning',
      LOW: 'danger',
    };
    return map[quality] ?? 'neutral';
  }
}
