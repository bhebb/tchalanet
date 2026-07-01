import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
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
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute } from '@angular/router';
import { SlicePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import type { AdminStatusTone } from '@tch/ui/console';
import {
  AdminLimitsApi,
  BreachOutcome,
  LimitAssignmentItem,
  LimitRuleSpec,
  TargetType,
} from './admin-limits-api.service';

const BREACH_OUTCOMES: BreachOutcome[] = ['ALLOW', 'WARN', 'REQUIRE_APPROVAL', 'BLOCK'];
const EDITABLE_TENANT_SCOPES = new Set<LimitPageScope>(['global']);

type LimitPageScope = 'system' | 'global' | 'seller' | 'number' | 'game' | 'draw';

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
    TchSectionError,
  ],
  templateUrl: './upsert-limit.dialog.html',
  styleUrl: './upsert-limit.dialog.scss',
})
export class UpsertLimitDialog {
  private readonly api = inject(AdminLimitsApi);
  private readonly ref = inject(MatDialogRef<UpsertLimitDialog>);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly breachOutcomes = BREACH_OUTCOMES;
  readonly spec = signal<LimitRuleSpec | null>(null);
  readonly assignment = signal<LimitAssignmentItem | null>(null);
  readonly targetType = signal<TargetType>('TENANT');
  readonly targetId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

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
    const spec = this.spec();
    if (this.form.invalid || !spec) return;
    let parsedParams: unknown;
    try { parsedParams = JSON.parse(this.form.controls.params.value); } catch { return; }
    this.saving.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.api.upsertAssignment({
      ruleKey: spec.ruleKey,
      targetType: this.targetType(),
      targetId: this.targetId() ?? undefined,
      enabled: v.enabled,
      onBreach: v.onBreach,
      params: parsedParams,
    }, { suppressShellFeedback: true }).subscribe({
      next: result => this.ref.close(result),
      error: (err: unknown) => {
        this.error.set(resolveLimitError(
          (err as { error?: ProblemDetail })?.error,
          `admin.limits.assignment.${this.spec()?.ruleKey ?? 'unknown'}`,
          'section',
          this.translate,
        ));
        this.saving.set(false);
      },
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
    TchSectionError,
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
  styleUrl: './admin-limits.page.scss',
})
export class AdminLimitsPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly scope = signal<LimitPageScope>('global');
  readonly rules = signal<LimitRuleSpec[]>([]);
  readonly assignments = signal<LimitAssignmentItem[]>([]);

  readonly displayedColumns = ['rule', 'category', 'status', 'onBreach', 'actions'];
  readonly canEditScope = computed(() => EDITABLE_TENANT_SCOPES.has(this.scope()));
  readonly showsAssignments = computed(() => this.scope() === 'global');
  readonly pageTitle = computed(() => {
    switch (this.scope()) {
      case 'system': return 'Catalogue des limites système';
      case 'seller': return 'Limites par vendeur';
      case 'number': return 'Limites par numéro';
      case 'game': return 'Limites par jeu';
      case 'draw': return 'Limites par tirage';
      case 'global':
      default: return 'Limites générales';
    }
  });
  readonly pageDescription = computed(() => {
    switch (this.scope()) {
      case 'system':
        return 'Consultez les règles supportées par le backend. Ces règles ne sont pas modifiables depuis le tenant.';
      case 'seller':
      case 'number':
      case 'game':
      case 'draw':
        return 'Ce scope nécessite de choisir une cible avant de configurer des limites spécifiques.';
      case 'global':
      default:
        return 'Configurez les limites tenant appliquées par défaut aux ventes.';
    }
  });

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
      const rows = byCategory.get(spec.category);
      if (rows) rows.push({ spec, assignment: assignMap.get(spec.ruleKey) ?? null });
    }
    return byCategory;
  });

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.scope.set(normalizeLimitScope(params.get('scope')));
        this.load();
      });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.listRules({ suppressShellFeedback: true }).subscribe({
      next: rules => {
        this.rules.set(rules);
        if (this.showsAssignments()) {
          this.loadAssignments();
        } else {
          this.assignments.set([]);
          this.loading.set(false);
        }
      },
      error: (err: unknown) => {
        this.error.set(resolveLimitError(
          (err as { error?: ProblemDetail })?.error,
          'admin.limits.rules',
          'page',
          this.translate,
        ));
        this.loading.set(false);
      },
    });
  }

  private loadAssignments(): void {
    this.api.listAssignments('TENANT', undefined, { suppressShellFeedback: true }).subscribe({
      next: view => { this.assignments.set(view.items); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(resolveLimitError(
          (err as { error?: ProblemDetail })?.error,
          'admin.limits.assignments',
          'page',
          this.translate,
        ));
        this.loading.set(false);
      },
    });
  }

  openUpsert(row: RuleRow): void {
    if (!this.canEditScope()) return;
    const ref = this.dialog.open(UpsertLimitDialog, { width: '560px' });
    (ref.componentInstance as UpsertLimitDialog).init(row.spec, 'TENANT', null, row.assignment);
    ref.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.actionError.set(null);
        this.actionNotice.set('Règle enregistrée.');
        this.loadAssignments();
      }
    });
  }

  delete(row: RuleRow): void {
    if (!this.canEditScope()) return;
    if (!row.assignment) return;
    if (!confirm(`Supprimer la règle ${row.spec.ruleKey} ?`)) return;
    this.actionError.set(null);
    this.actionNotice.set(null);
    this.api.deleteAssignment(row.assignment.id.value, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.actionNotice.set('Règle supprimée.');
        this.loadAssignments();
      },
      error: (err: unknown) => {
        this.actionError.set(resolveLimitError(
          (err as { error?: ProblemDetail })?.error,
          `admin.limits.delete.${row.spec.ruleKey}`,
          'section',
          this.translate,
        ));
      },
    });
  }

  assignmentTone(row: RuleRow): AdminStatusTone {
    if (!this.showsAssignments()) return 'neutral';
    if (!row.assignment) return 'neutral';
    return row.assignment.enabled ? 'success' : 'warning';
  }

  assignmentLabel(row: RuleRow): string {
    if (this.scope() === 'system') return 'Catalogue';
    if (!this.showsAssignments()) return 'Sélection requise';
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

  categoryLabel(category: string): string {
    return category
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
      .join(' ');
  }
}

function normalizeLimitScope(value: string | null): LimitPageScope {
  switch ((value ?? 'global').toLowerCase()) {
    case 'system': return 'system';
    case 'seller': return 'seller';
    case 'number': return 'number';
    case 'game': return 'game';
    case 'draw': return 'draw';
    case 'global':
    default: return 'global';
  }
}

function resolveLimitError(
  problem: ProblemDetail | undefined,
  source: string,
  surface: 'page' | 'section',
  translate: TranslateService,
): ErrorViewModel {
  if (!problem) {
    return {
      severity: 'error',
      title: translate.instant('common.errors.fallback.title'),
      message: translate.instant('common.errors.fallback.message'),
    };
  }

  const normalized = webAppErrorFromProblemDetail(problem, source, surface);
  const copy = resolveErrorFeedbackCopy(normalized, key => translate.instant(key));
  return toErrorViewModel(normalized, copy);
}
