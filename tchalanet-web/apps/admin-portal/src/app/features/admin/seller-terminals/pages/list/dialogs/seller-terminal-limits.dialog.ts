import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
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
  templateUrl: './seller-terminal-limits.dialog.html',
  styleUrls: ['./seller-terminal-limits.dialog.scss'],
})
export class SellerTerminalLimitsDialog implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly terminal = inject<SellerTerminalSummaryRow>(MAT_DIALOG_DATA);

  readonly breachOutcomes = BREACH_OUTCOMES;
  readonly displayedColumns = ['rule', 'onBreach', 'status', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
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
    this.actionError.set(null);

    const specs$ = this.api.listRules({ suppressShellFeedback: true });
    const assignments$ = this.api.listAssignments(
      'SELLER_TERMINAL',
      this.terminal.id.value,
      { suppressShellFeedback: true },
    );

    let specsLoaded = false;
    let assignmentsLoaded = false;
    let failed = false;

    const tryFinish = () => {
      if (specsLoaded && assignmentsLoaded) this.loading.set(false);
    };

    specs$.subscribe({
      next: specs => { this.allSpecs.set(specs); specsLoaded = true; if (!failed) tryFinish(); },
      error: err => { if (!failed) { failed = true; this.error.set(this.errorViewModel(err, 'admin.sellerTerminal.limits.rules')); this.loading.set(false); } },
    });

    assignments$.subscribe({
      next: view => { this.assignments.set(view.items); assignmentsLoaded = true; if (!failed) tryFinish(); },
      error: err => { if (!failed) { failed = true; this.error.set(this.errorViewModel(err, 'admin.sellerTerminal.limits.assignments')); this.loading.set(false); } },
    });
  }

  editRow(row: AssignmentRow): void {
    this.actionError.set(null);
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
    this.actionError.set(null);
  }

  saveEdit(): void {
    const row = this.editingRow();
    if (!row || this.form.invalid) return;
    const v = this.form.getRawValue();
    let parsedParams: unknown;
    try { parsedParams = JSON.parse(v.params); } catch { return; }

    this.saving.set(true);
    this.actionError.set(null);
    const req: UpsertLimitAssignmentRequest = {
      ruleKey:    row.spec.ruleKey,
      targetType: 'SELLER_TERMINAL',
      targetId:   this.terminal.id.value,
      enabled:    v.enabled,
      onBreach:   v.onBreach,
      params:     parsedParams,
    };

    this.api.upsertAssignment(req, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.saving.set(false);
        this.editingRow.set(null);
        this.selectedRuleKey = null;
        this.reloadAssignments();
      },
      error: err => {
        this.saving.set(false);
        this.actionError.set(this.errorViewModel(err, 'admin.sellerTerminal.limits.save'));
      },
    });
  }

  deleteRow(row: AssignmentRow): void {
    if (!row.assignment) return;
    this.actionError.set(null);
    this.api.deleteAssignment(row.assignment.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.reloadAssignments();
      },
      error: err => this.actionError.set(this.errorViewModel(err, 'admin.sellerTerminal.limits.delete')),
    });
  }

  addRule(): void {
    const spec = this.allSpecs().find(s => s.ruleKey === this.selectedRuleKey);
    if (!spec) return;
    this.editRow({ spec, assignment: null });
  }

  private reloadAssignments(): void {
    this.api.listAssignments('SELLER_TERMINAL', this.terminal.id.value, { suppressShellFeedback: true }).subscribe({
      next: view => this.assignments.set(view.items),
      error: err => this.actionError.set(this.errorViewModel(err, 'admin.sellerTerminal.limits.reload')),
    });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
