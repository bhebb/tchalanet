import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import {
  AdminLimitsApi,
  BreachOutcome,
  LimitAssignmentItem,
  LimitRuleSpec,
  UpsertLimitAssignmentRequest,
} from '../../../../pages/limits/admin-limits-api.service';
import { SellerTerminalSummaryRow } from '../../../../seller-terminal-api.service';

const BREACH_OUTCOMES: BreachOutcome[] = ['ALLOW', 'WARN', 'REQUIRE_APPROVAL', 'BLOCK'];

function jsonValidator(ctrl: { value: string }) {
  try { JSON.parse(ctrl.value); return null; } catch { return { jsonInvalid: true }; }
}

interface AssignmentRow {
  spec: LimitRuleSpec;
  assignment: LimitAssignmentItem | null;
}

@Component({
  selector: 'tch-seller-terminal-limits-dialog',
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
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    TchLoading,
    TchErrorPanel,
  ],
  template: `
    <h2 mat-dialog-title>
      Limites — {{ terminal.displayName }}
      <span style="font-size:0.8rem;font-weight:400;color:var(--tch-text-secondary,#666);margin-left:8px">
        {{ terminal.terminalCode }}
      </span>
    </h2>

    <mat-dialog-content style="min-width:640px;max-width:760px">
      @if (loading()) {
        <tch-loading />
      } @else if (error()) {
        <tch-error-panel [message]="error()!" (retry)="load()" />
      } @else {
        @if (rows().length === 0 && !editingRow()) {
          <p style="color:var(--tch-text-secondary,#666);font-size:0.9rem;margin:8px 0 16px">
            Aucune limite spécifique configurée pour ce terminal. Les règles tenant s'appliquent.
          </p>
        }

        @if (rows().length > 0) {
          <table mat-table [dataSource]="rows()" style="width:100%;margin-bottom:16px">
            <ng-container matColumnDef="rule">
              <th mat-header-cell *matHeaderCellDef>Règle</th>
              <td mat-cell *matCellDef="let r">
                <code style="font-size:0.78rem">{{ r.spec.ruleKey }}</code>
                @if (r.spec.label) {
                  <div style="font-size:0.8rem;color:var(--tch-text-secondary,#666)">{{ r.spec.label }}</div>
                }
              </td>
            </ng-container>

            <ng-container matColumnDef="onBreach">
              <th mat-header-cell *matHeaderCellDef>Action</th>
              <td mat-cell *matCellDef="let r">
                <span style="font-size:0.85rem">{{ r.assignment?.onBreach ?? '—' }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Statut</th>
              <td mat-cell *matCellDef="let r">
                @if (r.assignment) {
                  <span style="font-size:0.82rem;color:{{ r.assignment.enabled ? 'var(--tch-color-success,green)' : 'var(--tch-text-secondary,#666)' }}">
                    {{ r.assignment.enabled ? 'Actif' : 'Inactif' }}
                  </span>
                } @else {
                  <span style="font-size:0.82rem;color:var(--tch-text-secondary,#666)">—</span>
                }
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let r" style="white-space:nowrap;text-align:right">
                <button mat-icon-button (click)="editRow(r)" matTooltip="Modifier">
                  <mat-icon>edit</mat-icon>
                </button>
                @if (r.assignment) {
                  <button mat-icon-button color="warn" (click)="deleteRow(r)" matTooltip="Supprimer">
                    <mat-icon>delete</mat-icon>
                  </button>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>
        }

        @if (editingRow()) {
          <div style="border:1px solid var(--mat-divider-color,#e0e0e0);border-radius:8px;padding:16px;margin-bottom:16px">
            <div style="font-size:0.85rem;color:var(--tch-text-secondary,#666);margin-bottom:12px">
              <strong>{{ editingRow()!.spec.ruleKey }}</strong>
              @if (editingRow()!.spec.label) { — {{ editingRow()!.spec.label }} }
            </div>
            @if (editingRow()!.spec.description) {
              <p style="font-size:0.82rem;margin:0 0 12px;color:var(--tch-text-secondary,#666)">
                {{ editingRow()!.spec.description }}
              </p>
            }
            <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px">
              <mat-form-field appearance="outline">
                <mat-label>Action en cas de dépassement</mat-label>
                <mat-select formControlName="onBreach">
                  @for (o of breachOutcomes; track o) {
                    <mat-option [value]="o">{{ o }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Paramètres (JSON)</mat-label>
                <textarea matInput formControlName="params" rows="4"
                  style="font-family:monospace;font-size:0.82rem"></textarea>
                @if (form.controls['params'].errors?.['jsonInvalid']) {
                  <mat-error>JSON invalide.</mat-error>
                }
              </mat-form-field>
              <mat-checkbox formControlName="enabled">Actif</mat-checkbox>
            </form>
            <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px">
              <button mat-stroked-button (click)="cancelEdit()">Annuler</button>
              <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="saveEdit()">
                Enregistrer
              </button>
            </div>
          </div>
        }

        @if (!editingRow()) {
          <div style="margin-bottom:8px">
            <mat-form-field appearance="outline" style="width:100%">
              <mat-label>Ajouter une règle</mat-label>
              <mat-select [(value)]="selectedRuleKey">
                @for (spec of availableSpecs(); track spec.ruleKey) {
                  <mat-option [value]="spec.ruleKey">
                    {{ spec.ruleKey }} — {{ spec.label }}
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>
            <button mat-stroked-button [disabled]="!selectedRuleKey" (click)="addRule()">
              <mat-icon>add</mat-icon> Configurer
            </button>
          </div>
        }
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
})
export class SellerTerminalLimitsDialog implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<SellerTerminalLimitsDialog>);

  readonly terminal = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);

  readonly breachOutcomes = BREACH_OUTCOMES;
  readonly displayedColumns = ['rule', 'onBreach', 'status', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);

  private readonly allSpecs = signal<LimitRuleSpec[]>([]);
  private readonly assignments = signal<LimitAssignmentItem[]>([]);

  readonly rows = computed<AssignmentRow[]>(() => {
    const assignedByKey = new Map(this.assignments().map(a => [a.ruleKey, a]));
    return this.assignments().map(a => ({
      spec: this.allSpecs().find(s => s.ruleKey === a.ruleKey) ?? {
        ruleKey: a.ruleKey,
        label: a.ruleKey,
        description: '',
        defaultOutcome: a.onBreach,
        category: '',
        stateless: true,
        paramsTemplate: {},
      } as LimitRuleSpec,
      assignment: assignedByKey.get(a.ruleKey) ?? null,
    }));
  });

  readonly availableSpecs = computed<LimitRuleSpec[]>(() => {
    const assigned = new Set(this.assignments().map(a => a.ruleKey));
    return this.allSpecs().filter(s => !assigned.has(s.ruleKey));
  });

  readonly editingRow = signal<AssignmentRow | null>(null);
  selectedRuleKey: string | null = null;

  readonly form = this.fb.nonNullable.group({
    onBreach: ['BLOCK' as BreachOutcome, Validators.required],
    params:   ['{}', [Validators.required, jsonValidator]],
    enabled:  [true],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    const specs$ = this.api.listRules();
    const assignments$ = this.api.listAssignments('SELLER_TERMINAL', this.terminal.id.value);

    let specsLoaded = false;
    let assignmentsLoaded = false;
    let failed = false;

    const tryFinish = () => {
      if (specsLoaded && assignmentsLoaded) this.loading.set(false);
    };

    specs$.subscribe({
      next: specs => { this.allSpecs.set(specs); specsLoaded = true; if (!failed) tryFinish(); },
      error: () => { if (!failed) { failed = true; this.error.set('Erreur de chargement.'); this.loading.set(false); } },
    });

    assignments$.subscribe({
      next: view => { this.assignments.set(view.items); assignmentsLoaded = true; if (!failed) tryFinish(); },
      error: () => { if (!failed) { failed = true; this.error.set('Erreur de chargement.'); this.loading.set(false); } },
    });
  }

  editRow(row: AssignmentRow): void {
    this.editingRow.set(row);
    const a = row.assignment;
    this.form.patchValue({
      onBreach: a?.onBreach ?? row.spec.defaultOutcome,
      params:   a ? JSON.stringify(a.params, null, 2) : JSON.stringify(row.spec.paramsTemplate ?? {}, null, 2),
      enabled:  a?.enabled ?? true,
    });
  }

  cancelEdit(): void {
    this.editingRow.set(null);
    this.selectedRuleKey = null;
  }

  saveEdit(): void {
    const row = this.editingRow();
    if (!row || this.form.invalid) return;
    let parsedParams: unknown;
    try { parsedParams = JSON.parse(this.form.value.params!); } catch { return; }

    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: UpsertLimitAssignmentRequest = {
      ruleKey:    row.spec.ruleKey,
      targetType: 'SELLER_TERMINAL',
      targetId:   this.terminal.id.value,
      enabled:    v.enabled,
      onBreach:   v.onBreach,
      params:     parsedParams,
    };

    this.api.upsertAssignment(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.editingRow.set(null);
        this.selectedRuleKey = null;
        this.snackBar.open('Règle enregistrée.', 'OK', { duration: 3000 });
        this.reloadAssignments();
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Erreur lors de l\'enregistrement.', 'OK', { duration: 4000 });
      },
    });
  }

  deleteRow(row: AssignmentRow): void {
    if (!row.assignment) return;
    this.api.deleteAssignment(row.assignment.id.value).subscribe({
      next: () => {
        this.snackBar.open('Règle supprimée.', 'OK', { duration: 3000 });
        this.reloadAssignments();
      },
      error: () => this.snackBar.open('Erreur lors de la suppression.', 'OK', { duration: 4000 }),
    });
  }

  addRule(): void {
    const spec = this.allSpecs().find(s => s.ruleKey === this.selectedRuleKey);
    if (!spec) return;
    this.editRow({ spec, assignment: null });
  }

  private reloadAssignments(): void {
    this.api.listAssignments('SELLER_TERMINAL', this.terminal.id.value).subscribe({
      next: view => this.assignments.set(view.items),
      error: () => {},
    });
  }
}
