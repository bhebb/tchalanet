import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import {
  BusinessDaysApiService,
  BusinessDayView,
  UpsertBusinessDayRequest,
} from '../../business-days-api.service';

// ── Add override dialog ────────────────────────────────────────────────────────

@Component({
  selector: 'tch-add-business-day-dialog',
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
    <h2 mat-dialog-title>Ajouter une exception</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Date (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="date" placeholder="2025-01-01" />
          @if (form.controls.date.invalid && form.controls.date.touched) {
            <mat-error>Date requise (format YYYY-MM-DD).</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Statut</mat-label>
          <mat-select formControlName="status">
            <mat-option value="OPEN">OPEN (jour ouvrable)</mat-option>
            <mat-option value="CLOSED">CLOSED (jour fermé)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Raison (optionnel)</mat-label>
          <input matInput formControlName="reason" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="submit()">
        Ajouter
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 360px; }
      .full-width { width: 100%; }
    `,
  ],
})
export class AddBusinessDayDialog {
  private readonly dialogRef = inject(MatDialogRef<AddBusinessDayDialog>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    date: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]],
    status: ['CLOSED' as 'OPEN' | 'CLOSED', Validators.required],
    reason: [''],
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    const req: UpsertBusinessDayRequest = {
      date: v.date!,
      status: v.status!,
      reason: v.reason || undefined,
    };
    this.dialogRef.close(req);
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

function startOfMonth(year: number, month: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-01`;
}

function endOfMonth(year: number, month: number): string {
  const last = new Date(year, month + 1, 0).getDate();
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(last).padStart(2, '0')}`;
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month + 1, 0).getDate();
}

@Component({
  selector: 'tch-admin-business-days-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Jours ouvrables"
      description="Gestion du calendrier des jours ouvrables."
    >
      <div actions>
        <button mat-flat-button color="primary" (click)="addOverride()">
          <span class="material-symbols-outlined">add</span>
          Ajouter une exception
        </button>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <span class="material-symbols-outlined spin">progress_activity</span>
          Chargement...
        </div>
      } @else if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
        </div>
      } @else {
        <!-- Month navigation -->
        <div class="month-nav">
          <button mat-icon-button (click)="prevMonth()">
            <span class="material-symbols-outlined">chevron_left</span>
          </button>
          <strong>{{ monthLabel() }}</strong>
          <button mat-icon-button (click)="nextMonth()">
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </div>

        <!-- Calendar grid -->
        <div class="calendar-grid">
          @for (day of calendarDays(); track day.date) {
            <div class="calendar-day" [class.closed]="day.status === 'CLOSED'" [class.open]="day.status === 'OPEN' && day.override">
              <span class="day-num">{{ day.dayNum }}</span>
              @if (day.status === 'CLOSED') {
                <span class="day-chip closed">FERMÉ</span>
              }
              @if (day.override && day.status === 'OPEN') {
                <span class="day-chip open">OUVERT</span>
              }
            </div>
          }
        </div>

        <!-- Overrides table -->
        <h3 class="section-title" style="margin-top:2rem">Exceptions enregistrées</h3>
        @if (overrides().length === 0) {
          <tch-admin-empty-state
            icon="event_busy"
            title="Aucune exception"
            message="Aucune exception de calendrier n'a été configurée."
          />
        } @else {
          <table mat-table [dataSource]="overrides()">
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Date</th>
              <td mat-cell *matCellDef="let row">{{ row.date }}</td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Statut</th>
              <td mat-cell *matCellDef="let row">{{ row.status }}</td>
            </ng-container>
            <ng-container matColumnDef="reason">
              <th mat-header-cell *matHeaderCellDef>Raison</th>
              <td mat-cell *matCellDef="let row">{{ row.reason ?? '—' }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let row">
                <button mat-icon-button color="warn" (click)="deleteOverride(row)">
                  <span class="material-symbols-outlined">delete</span>
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="tableColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: tableColumns"></tr>
          </table>
        }
      }
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .loading-state {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 2rem;
        color: var(--tch-color-on-surface-variant);
      }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; }
      @keyframes spin { to { transform: rotate(360deg); } }
      .error-panel {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: 0.5rem;
        background: var(--tch-color-error-container, #ffdad6);
        color: var(--tch-color-on-error-container, #410002);
      }
      .month-nav {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 1rem;
      }
      .calendar-grid {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        gap: 0.375rem;
      }
      .calendar-day {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.2rem;
        padding: 0.5rem 0.25rem;
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.375rem;
        font-size: 0.8125rem;
        min-height: 3rem;
      }
      .calendar-day.closed {
        background: #ffdad6;
        border-color: #ba1a1a;
      }
      .calendar-day.open {
        background: #d4edda;
        border-color: #155724;
      }
      .day-num { font-weight: 600; }
      .day-chip {
        font-size: 0.625rem;
        font-weight: 700;
        text-transform: uppercase;
        padding: 0.1rem 0.35rem;
        border-radius: 9999px;
        letter-spacing: 0.04em;
      }
      .day-chip.closed { background: #ba1a1a; color: #fff; }
      .day-chip.open { background: #155724; color: #fff; }
      .section-title { font-size: 1rem; font-weight: 600; margin: 0 0 0.5rem; }
      table { width: 100%; }
    `,
  ],
})
export class AdminBusinessDaysPage implements OnInit {
  private readonly api = inject(BusinessDaysApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly tableColumns = ['date', 'status', 'reason', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly overrides = signal<BusinessDayView[]>([]);

  private readonly now = new Date();
  readonly currentYear = signal(this.now.getFullYear());
  readonly currentMonth = signal(this.now.getMonth()); // 0-based

  readonly monthLabel = computed(() => {
    const d = new Date(this.currentYear(), this.currentMonth(), 1);
    return d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  });

  readonly calendarDays = computed(() => {
    const year = this.currentYear();
    const month = this.currentMonth();
    const days = daysInMonth(year, month);
    const overrideMap = new Map(this.overrides().map(o => [o.date, o]));

    return Array.from({ length: days }, (_, i) => {
      const dayNum = i + 1;
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
      const override = overrideMap.get(dateStr);
      return {
        date: dateStr,
        dayNum,
        status: override?.status ?? null,
        override: !!override,
      };
    });
  });

  ngOnInit(): void {
    this.loadMonth();
  }

  prevMonth(): void {
    if (this.currentMonth() === 0) {
      this.currentYear.update(y => y - 1);
      this.currentMonth.set(11);
    } else {
      this.currentMonth.update(m => m - 1);
    }
    this.loadMonth();
  }

  nextMonth(): void {
    if (this.currentMonth() === 11) {
      this.currentYear.update(y => y + 1);
      this.currentMonth.set(0);
    } else {
      this.currentMonth.update(m => m + 1);
    }
    this.loadMonth();
  }

  private loadMonth(): void {
    const from = startOfMonth(this.currentYear(), this.currentMonth());
    const to = endOfMonth(this.currentYear(), this.currentMonth());
    this.loading.set(true);
    this.error.set(null);
    this.api.listBusinessDays({ from, to }).subscribe({
      next: v => { this.overrides.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  addOverride(): void {
    const ref = this.dialog.open(AddBusinessDayDialog, { width: '420px' });
    ref.afterClosed().subscribe((req: UpsertBusinessDayRequest | undefined) => {
      if (!req) return;
      this.api.upsertBusinessDay(req).subscribe({
        next: () => {
          this.snackBar.open('Exception ajoutée.', 'OK', { duration: 3000 });
          this.loadMonth();
        },
        error: () => this.snackBar.open('Erreur lors de l\'ajout.', 'OK', { duration: 4000 }),
      });
    });
  }

  deleteOverride(row: BusinessDayView): void {
    this.api.deleteBusinessDay(row.id).subscribe({
      next: () => {
        this.snackBar.open('Exception supprimée.', 'OK', { duration: 3000 });
        this.loadMonth();
      },
      error: () => this.snackBar.open('Erreur lors de la suppression.', 'OK', { duration: 4000 }),
    });
  }
}
