import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SlicePipe } from '@angular/common';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import type { AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  AdminLimitsApi,
  BreachOutcome,
  LimitAssignmentItem,
  LimitRuleSpec,
  RuleKey,
  TargetType,
} from './admin-limits-api.service';

const BREACH_OUTCOMES: BreachOutcome[] = ['ALLOW', 'WARN', 'REQUIRE_APPROVAL', 'BLOCK'];

// ── Upsert Dialog ─────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-upsert-limit-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ assignment() ? 'Modifier' : 'Configurer' }} la règle</h2>
    <mat-dialog-content>
      <div style="font-size:0.85rem;color:var(--tch-text-secondary,#666);padding:4px 0 12px">
        <strong>{{ spec()?.ruleKey }}</strong>
        @if (spec()?.label) { — {{ spec()!.label }} }
      </div>
      @if (spec()?.description) {
        <p style="font-size:0.82rem;margin:0 0 12px;color:var(--tch-text-secondary,#666)">{{ spec()!.description }}</p>
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
          <textarea matInput formControlName="params" rows="5" style="font-family:monospace;font-size:0.82rem"></textarea>
          @if (form.controls.params.errors?.['jsonInvalid']) {
            <mat-error>JSON invalide.</mat-error>
          }
        </mat-form-field>
        <mat-checkbox formControlName="enabled">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
})
export class UpsertLimitDialog {
  private readonly api = inject(AdminLimitsApi);
  private readonly ref = inject(MatDialogRef<UpsertLimitDialog>);
  private readonly fb = inject(FormBuilder);

  readonly breachOutcomes = BREACH_OUTCOMES;
  readonly spec = signal<LimitRuleSpec | null>(null);
  readonly assignment = signal<LimitAssignmentItem | null>(null);
  readonly targetType = signal<TargetType>('TENANT');
  readonly targetId = signal<string | null>(null);
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    onBreach: ['BLOCK' as BreachOutcome, Validators.required],
    params: ['{}', [Validators.required, jsonValidator]],
    enabled: [true],
  });

  init(spec: LimitRuleSpec, targetType: TargetType, targetId: string | null, assignment: LimitAssignmentItem | null): void {
    this.spec.set(spec);
    this.targetType.set(targetType);
    this.targetId.set(targetId);
    this.assignment.set(assignment);
    this.form.patchValue({
      onBreach: assignment?.onBreach ?? spec.defaultOutcome,
      params: assignment
        ? JSON.stringify(assignment.params, null, 2)
        : JSON.stringify(spec.paramsTemplate ?? {}, null, 2),
      enabled: assignment?.enabled ?? true,
    });
  }

  save(): void {
    if (this.form.invalid || !this.spec()) return;
    let parsedParams: unknown;
    try { parsedParams = JSON.parse(this.form.value.params!); } catch { return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    this.api.upsertAssignment({
      ruleKey: this.spec()!.ruleKey,
      targetType: this.targetType(),
      targetId: this.targetId() ?? undefined,
      enabled: v.enabled,
      onBreach: v.onBreach,
      params: parsedParams,
    }).subscribe({
      next: result => this.ref.close(result),
      error: () => { this.saving.set(false); },
    });
  }
}

function jsonValidator(ctrl: { value: string }) {
  try { JSON.parse(ctrl.value); return null; } catch { return { jsonInvalid: true }; }
}

// ── Main Page ─────────────────────────────────────────────────────────────────
interface RuleRow {
  spec: LimitRuleSpec;
  assignment: LimitAssignmentItem | null;
}

@Component({
  selector: 'tch-admin-limits-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatTabsModule,
    MatTooltipModule,
    ReactiveFormsModule,
    SlicePipe,
  ],
  templateUrl: './admin-limits.page.html',
})
export class AdminLimitsPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rules = signal<LimitRuleSpec[]>([]);
  readonly assignments = signal<LimitAssignmentItem[]>([]);

  readonly displayedColumns = ['rule', 'category', 'status', 'onBreach', 'actions'];

  readonly categories = computed(() => {
    const seen = new Set<string>();
    const cats: string[] = [];
    for (const r of this.rules()) {
      if (!seen.has(r.category)) { seen.add(r.category); cats.push(r.category); }
    }
    return cats;
  });

  readonly rowsByCategory = computed(() => {
    const assignMap = new Map(this.assignments().map(a => [a.ruleKey, a]));
    const byCategory = new Map<string, RuleRow[]>();
    for (const spec of this.rules()) {
      if (!byCategory.has(spec.category)) byCategory.set(spec.category, []);
      byCategory.get(spec.category)!.push({ spec, assignment: assignMap.get(spec.ruleKey) ?? null });
    }
    return byCategory;
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listRules().subscribe({
      next: rules => {
        this.rules.set(rules);
        this.loadAssignments();
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  private loadAssignments(): void {
    this.api.listAssignments('TENANT').subscribe({
      next: view => { this.assignments.set(view.items); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  openUpsert(row: RuleRow): void {
    const ref = this.dialog.open(UpsertLimitDialog, { width: '560px' });
    (ref.componentInstance as UpsertLimitDialog).init(row.spec, 'TENANT', null, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.snackBar.open('Règle enregistrée.', 'OK', { duration: 4000 });
        this.loadAssignments();
      }
    });
  }

  delete(row: RuleRow): void {
    if (!row.assignment) return;
    if (!confirm(`Supprimer la règle ${row.spec.ruleKey} ?`)) return;
    this.api.deleteAssignment(row.assignment.id.value).subscribe({
      next: () => {
        this.snackBar.open('Règle supprimée.', 'OK', { duration: 4000 });
        this.loadAssignments();
      },
      error: (err: unknown) => {
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.',
          'OK', { duration: 5000 });
      },
    });
  }

  assignmentTone(row: RuleRow): AdminStatusTone {
    if (!row.assignment) return 'neutral';
    return row.assignment.enabled ? 'success' : 'warning';
  }

  assignmentLabel(row: RuleRow): string {
    if (!row.assignment) return 'Non configurée';
    return row.assignment.enabled ? 'Active' : 'Désactivée';
  }

  breachTone(outcome: BreachOutcome | undefined): AdminStatusTone {
    if (!outcome) return 'neutral';
    if (outcome === 'BLOCK') return 'danger';
    if (outcome === 'REQUIRE_APPROVAL') return 'warning';
    if (outcome === 'WARN') return 'info';
    return 'neutral';
  }

  paramsPreview(row: RuleRow): string {
    if (!row.assignment?.params) return '';
    try {
      const p = row.assignment.params as Record<string, unknown>;
      return Object.entries(p).map(([k, v]) => `${k}: ${v}`).join(', ');
    } catch { return ''; }
  }
}
