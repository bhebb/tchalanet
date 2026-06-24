import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
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
  template: `
    <tch-admin-page-shell
      title="Guide des règles de limite"
      description="Référence complète des règles disponibles pour contrôler les mises, expositions et gains.">

      <div actions>
        <a mat-stroked-button [routerLink]="['../limits']">
          <span class="material-symbols-outlined">tune</span>
          Configurer les limites
        </a>
      </div>

      @if (loading()) {
        <tch-loading label="Chargement..." />
      } @else if (error()) {
        <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
      } @else {
        <div class="intro-box">
          <span class="material-symbols-outlined intro-icon">info</span>
          <div>
            <strong>Comment fonctionnent les limites ?</strong>
            <p>
              Chaque règle est assignée à un <em>scope</em> : tenant global, channel, ou vendeur.
              Lors d'une vente, le moteur sélectionne la règle la plus spécifique (vendeur &gt; channel &gt; tenant).
              Si la règle est franchie, l'action définie (<em>action en cas de dépassement</em>) s'applique :
              blocage, approbation requise, avertissement, ou simple observation.
            </p>
            <p>
              Les règles d'<strong>exposition</strong> comparent les ventes déjà enregistrées pour un tirage donné —
              elles nécessitent donc des données historiques et ne s'appliquent qu'après la première vente.
            </p>
          </div>
        </div>

        <mat-accordion multi>
          @for (cat of categories(); track cat) {
            <mat-expansion-panel [expanded]="true">
              <mat-expansion-panel-header>
                <mat-panel-title class="category-title">{{ cat }}</mat-panel-title>
                <mat-panel-description>{{ rulesByCategory().get(cat)?.length }} règle(s)</mat-panel-description>
              </mat-expansion-panel-header>

              <div class="rules-grid">
                @for (rule of rulesByCategory().get(cat); track rule.ruleKey) {
                  <div class="rule-card">
                    <div class="rule-header">
                      <code class="rule-key">{{ rule.ruleKey }}</code>
                      @if (rule.stateless) {
                        <span class="badge badge--stateless">Stateless</span>
                      } @else {
                        <span class="badge badge--exposure">Exposition</span>
                      }
                    </div>
                    @if (rule.label) {
                      <div class="rule-label">{{ rule.label }}</div>
                    }
                    @if (rule.description) {
                      <p class="rule-desc">{{ rule.description }}</p>
                    }
                    <div class="rule-footer">
                      <span class="rule-footer-label">Action par défaut :</span>
                      <tch-admin-status-pill [tone]="outcomeTone(rule.defaultOutcome)" [label]="rule.defaultOutcome" />
                      @if (rule.paramsTemplate) {
                        <span class="rule-footer-label params-label">Paramètres :</span>
                        <code class="params-code">{{ paramsStr(rule) }}</code>
                      }
                    </div>
                  </div>
                }
              </div>
            </mat-expansion-panel>
          }
        </mat-accordion>

        <div class="outcomes-legend">
          <h3>Actions en cas de dépassement</h3>
          <div class="legend-grid">
            <div class="legend-item">
              <tch-admin-status-pill tone="neutral" label="ALLOW" />
              <span>La vente passe. Aucun effet.</span>
            </div>
            <div class="legend-item">
              <tch-admin-status-pill tone="info" label="WARN" />
              <span>La vente passe, un avertissement est remonté au vendeur.</span>
            </div>
            <div class="legend-item">
              <tch-admin-status-pill tone="warning" label="REQUIRE_APPROVAL" />
              <span>La vente est mise en attente jusqu'à approbation manuelle.</span>
            </div>
            <div class="legend-item">
              <tch-admin-status-pill tone="danger" label="BLOCK" />
              <span>La vente est refusée immédiatement.</span>
            </div>
          </div>
        </div>
      }
    </tch-admin-page-shell>

    <style>
      .intro-box {
        display: flex; gap: 12px; padding: 16px;
        background: var(--tch-surface-variant, #f5f5f5);
        border-radius: 8px; margin-bottom: 24px;
      }
      .intro-icon { font-size: 24px; color: var(--tch-primary, #1976d2); flex-shrink: 0; margin-top: 2px; }
      .intro-box p { margin: 4px 0 0; font-size: 0.88rem; color: var(--tch-text-secondary, #555); line-height: 1.5; }
      .category-title { font-weight: 600; text-transform: capitalize; }
      .rules-grid { display: flex; flex-direction: column; gap: 12px; padding: 8px 0; }
      .rule-card {
        border: 1px solid var(--tch-border, #e0e0e0); border-radius: 6px;
        padding: 14px 16px; background: var(--tch-surface, #fff);
      }
      .rule-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
      .rule-key { font-size: 0.82rem; font-weight: 600; color: var(--tch-primary, #1976d2); }
      .badge { font-size: 0.68rem; padding: 1px 6px; border-radius: 4px; font-weight: 500; }
      .badge--stateless { background: #e3f2fd; color: #1565c0; }
      .badge--exposure { background: #fff3e0; color: #e65100; }
      .rule-label { font-size: 0.88rem; font-weight: 500; margin-bottom: 4px; }
      .rule-desc { margin: 0 0 10px; font-size: 0.82rem; color: var(--tch-text-secondary, #555); line-height: 1.5; }
      .rule-footer { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 0.78rem; }
      .rule-footer-label { color: var(--tch-text-secondary, #666); }
      .params-label { margin-left: 8px; }
      .params-code { font-size: 0.72rem; background: var(--tch-surface-variant, #f5f5f5); padding: 2px 6px; border-radius: 4px; }
      .outcomes-legend { margin-top: 32px; padding: 16px; background: var(--tch-surface-variant, #f5f5f5); border-radius: 8px; }
      .outcomes-legend h3 { margin: 0 0 12px; font-size: 0.95rem; }
      .legend-grid { display: flex; flex-direction: column; gap: 8px; }
      .legend-item { display: flex; align-items: center; gap: 10px; font-size: 0.85rem; }
    </style>
  `,
})
export class AdminLimitsRulesPage implements OnInit {
  private readonly api = inject(AdminLimitsApi);

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
      map.get(r.category)!.push(r);
    }
    return map;
  });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listRules().subscribe({
      next: rules => { this.rules.set(rules); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
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
}
