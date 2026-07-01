import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent, AdminStatusPillComponent } from '@tch/ui/console';
import type { AdminStatusTone } from '@tch/ui/console';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { BreachOutcome, LimitAssignmentItem, LimitRuleSpec, TargetType } from '../../data-access/admin-limits.models';

const SIM_SCOPE_OPTIONS: { value: TargetType; label: string; requiresId: boolean }[] = [
  { value: 'TENANT',          label: 'Global',              requiresId: false },
  { value: 'DRAW_CHANNEL',    label: 'Par tirage',          requiresId: true  },
  { value: 'SELLER_TERMINAL', label: 'Par vendeur',         requiresId: true  },
];

@Component({
  selector: 'tch-admin-limits-system-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    TchErrorPanel,
    TchLoading,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
  ],
  templateUrl: './admin-limits-system.page.html',
  styleUrl: './admin-limits-system.page.scss',
})
export class AdminLimitsSystemPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly rules = signal<LimitRuleSpec[]>([]);

  readonly displayedColumns = ['rule', 'description', 'default', 'type'];
  readonly simDisplayedColumns = ['rule', 'outcome', 'params'];

  readonly categories = computed(() => {
    const seen = new Set<string>();
    const result: string[] = [];
    for (const r of this.rules()) {
      if (!seen.has(r.category)) { seen.add(r.category); result.push(r.category); }
    }
    return result;
  });

  readonly rulesByCategory = computed(() => {
    const map = new Map<string, LimitRuleSpec[]>();
    for (const r of this.rules()) {
      if (!map.has(r.category)) map.set(r.category, []);
      const bucket = map.get(r.category);
      if (bucket) bucket.push(r);
    }
    return map;
  });

  readonly rulesIndex = computed(() => {
    const map = new Map<string, LimitRuleSpec>();
    for (const r of this.rules()) map.set(r.ruleKey, r);
    return map;
  });

  // ── Simulation ──────────────────────────────────────────────────────────────

  readonly simScopeOptions = SIM_SCOPE_OPTIONS;
  readonly simScopeType = signal<TargetType>('TENANT');
  readonly simTargetId = signal('');
  readonly simLoading = signal(false);
  readonly simError = signal<ErrorViewModel | null>(null);
  readonly simAssignments = signal<LimitAssignmentItem[]>([]);
  readonly simLoaded = signal(false);

  readonly simRequiresId = computed(() =>
    SIM_SCOPE_OPTIONS.find(o => o.value === this.simScopeType())?.requiresId ?? false,
  );

  readonly simCanLoad = computed(() =>
    !this.simLoading() && (!this.simRequiresId() || this.simTargetId().trim().length > 0),
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.api.listRules({ suppressShellFeedback: true }).subscribe({
      next: rules => { this.rules.set(rules); this.loading.set(false); },
      error: (err: unknown) => {
        this.pageError.set(this.toPageError(err, 'admin.limits.system'));
        this.loading.set(false);
      },
    });
  }

  simulate(): void {
    if (!this.simCanLoad()) return;
    const targetId = this.simRequiresId() ? this.simTargetId().trim() : undefined;
    this.simLoading.set(true);
    this.simError.set(null);
    this.simLoaded.set(false);
    this.api.listAssignments(this.simScopeType(), targetId, { suppressShellFeedback: true }).subscribe({
      next: view => {
        this.simAssignments.set(view.items.filter(i => i.enabled));
        this.simLoading.set(false);
        this.simLoaded.set(true);
      },
      error: (err: unknown) => {
        this.simError.set(this.toPageError(err, 'admin.limits.simulate'));
        this.simLoading.set(false);
      },
    });
  }

  onSimScopeChange(): void {
    this.simTargetId.set('');
    this.simLoaded.set(false);
    this.simAssignments.set([]);
    this.simError.set(null);
  }

  categoryLabel(category: string): string {
    return category.split('_').map(p => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase()).join(' ');
  }

  defaultTone(outcome: BreachOutcome): AdminStatusTone {
    if (outcome === 'BLOCK') return 'danger';
    if (outcome === 'WARN') return 'info';
    if (outcome === 'REQUIRE_APPROVAL') return 'warning';
    return 'neutral';
  }

  defaultLabel(outcome: BreachOutcome): string {
    if (outcome === 'BLOCK') return 'Bloquer';
    if (outcome === 'WARN') return 'Avertir';
    if (outcome === 'REQUIRE_APPROVAL') return 'Approbation';
    if (outcome === 'ALLOW') return 'Autoriser';
    return outcome;
  }

  ruleLabel(ruleKey: string): string {
    return this.rulesIndex().get(ruleKey)?.label || ruleKey;
  }

  paramsDisplay(params: unknown): string {
    if (!params || (typeof params === 'object' && Object.keys(params as object).length === 0)) return '—';
    return JSON.stringify(params);
  }

  private toPageError(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }
    return {
      severity: 'error',
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
    };
  }
}
