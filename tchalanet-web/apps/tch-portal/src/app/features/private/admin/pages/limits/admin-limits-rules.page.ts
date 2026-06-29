import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../core/api/error-feedback-copy';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import type { AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import { AdminLimitsApi, BreachOutcome, LimitRuleSpec } from './admin-limits-api.service';

@Component({
  selector: 'tch-admin-limits-rules-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    RouterLink,
  ],
  templateUrl: './admin-limits-rules.page.html',
  styleUrl: './admin-limits-rules.page.scss',
})
export class AdminLimitsRulesPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rules = signal<LimitRuleSpec[]>([]);

  readonly categories = computed(() => {
    const seen = new Set<string>();
    const cats: string[] = [];
    for (const r of this.rules()) {
      if (!seen.has(r.category)) { seen.add(r.category); cats.push(r.category); }
    }
    return cats;
  });

  readonly rulesByCategory = computed(() => {
    const map = new Map<string, LimitRuleSpec[]>();
    for (const r of this.rules()) {
      if (!map.has(r.category)) map.set(r.category, []);
      const rules = map.get(r.category);
      if (rules) rules.push(r);
    }
    return map;
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listRules({ suppressShellFeedback: true }).subscribe({
      next: rules => { this.rules.set(rules); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorTitle((err as { error?: ProblemDetail })?.error));
        this.loading.set(false);
      },
    });
  }

  outcomeTone(outcome: BreachOutcome): AdminStatusTone {
    if (outcome === 'BLOCK') return 'danger';
    if (outcome === 'REQUIRE_APPROVAL') return 'warning';
    if (outcome === 'WARN') return 'info';
    return 'neutral';
  }

  paramsStr(rule: LimitRuleSpec): string {
    if (!rule.paramsTemplate) return '';
    try {
      return JSON.stringify(rule.paramsTemplate);
    } catch { return ''; }
  }

  private errorTitle(problem: ProblemDetail | undefined): string {
    if (!problem) return this.translate.instant('common.errors.categories.unexpected.title');

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.rules', 'page');
    return resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key)).title;
  }
}
