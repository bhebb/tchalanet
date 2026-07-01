import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
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
import type { BreachOutcome, LimitRuleSpec } from '../../data-access/admin-limits.models';

@Component({
  selector: 'tch-admin-limits-system-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatExpansionModule,
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

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.api.listRules({ suppressShellFeedback: true }).subscribe({
      next: rules => { this.rules.set(rules); this.loading.set(false); },
      error: (err: unknown) => {
        const problem = (err as { error?: ProblemDetail })?.error;
        if (problem) {
          const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.system', 'page');
          const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
          this.pageError.set(toErrorViewModel(normalized, copy));
        } else {
          this.pageError.set({
            severity: 'error',
            title: this.translate.instant('common.errors.fallback.title'),
            message: this.translate.instant('common.errors.fallback.message'),
          });
        }
        this.loading.set(false);
      },
    });
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
}
