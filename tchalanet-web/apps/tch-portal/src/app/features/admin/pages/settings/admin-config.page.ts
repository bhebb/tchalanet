import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { TenantConfigApiService } from '../../tenant-config-api.service';

type HealthState = 'valid' | 'invalid' | 'unknown';

interface ConfigHealth {
  locale: HealthState;
  communication: HealthState;
  document: HealthState;
  rules: HealthState;
  games: HealthState;
}

@Component({
  selector: 'tch-admin-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Configuration tenant"
      description="Paramètres de configuration du tenant."
    >
      <div actions>
        <button mat-stroked-button [disabled]="loading()" (click)="load()">
          <span class="material-symbols-outlined">refresh</span>
          Rafraîchir
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
          @if (traceId()) {
            <span class="trace-id">ID: {{ traceId() }}</span>
          }
        </div>
      } @else {
        <!-- Health card -->
        <div class="health-card">
          <h3 class="section-title">Santé de la configuration</h3>
          <div class="health-grid">
            @for (item of healthItems(); track item.key) {
              <div class="health-item">
                <span class="health-icon" [attr.data-state]="item.state">
                  {{ item.state === 'valid' ? 'check_circle' : item.state === 'invalid' ? 'error' : 'help' }}
                </span>
                {{ item.label }}
              </div>
            }
          </div>
        </div>

        <mat-tab-group class="config-tabs">
          <!-- Locale -->
          <mat-tab label="Locale">
            @if (config()) {
              <div class="tab-content">
                <pre class="json-block">{{ localeJson() }}</pre>
              </div>
            }
          </mat-tab>

          <!-- Communication -->
          <mat-tab label="Communication">
            @if (commConfig()) {
              <div class="tab-content">
                <pre class="json-block">{{ commJson() }}</pre>
              </div>
            } @else {
              <div class="tab-content">
                <tch-admin-empty-state
                  icon="notifications"
                  title="Non configuré"
                  message="Aucune configuration de communication disponible."
                />
              </div>
            }
          </mat-tab>

          <!-- Document -->
          <mat-tab label="Document">
            @if (docConfig()) {
              <div class="tab-content">
                <pre class="json-block">{{ docJson() }}</pre>
              </div>
            } @else {
              <div class="tab-content">
                <tch-admin-empty-state
                  icon="description"
                  title="Non configuré"
                  message="Aucune configuration de document disponible."
                />
              </div>
            }
          </mat-tab>

          <!-- Rules -->
          <mat-tab label="Règles">
            <div class="tab-content">
              <tch-admin-empty-state
                icon="rule"
                title="Règles métier"
                message="Affichage des règles métier bientôt disponible."
              />
            </div>
          </mat-tab>

          <!-- Raw JSON -->
          <mat-tab label="JSON brut">
            <div class="tab-content">
              <pre class="json-block">{{ rawJson() }}</pre>
            </div>
          </mat-tab>
        </mat-tab-group>
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
        margin-bottom: 1rem;
      }
      .trace-id { font-size: 0.75rem; opacity: 0.7; }
      .health-card {
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.75rem;
        padding: 1.25rem;
        margin-bottom: 1.5rem;
      }
      .section-title { margin: 0 0 0.75rem; font-size: 1rem; font-weight: 600; }
      .health-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }
      .health-item {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        font-size: 0.875rem;
      }
      .health-icon {
        font-family: 'Material Symbols Outlined';
        font-size: 1.125rem;
        font-variation-settings: 'FILL' 1;
      }
      .health-icon[data-state='valid'] { color: #155724; }
      .health-icon[data-state='invalid'] { color: #721c24; }
      .health-icon[data-state='unknown'] { color: var(--tch-color-on-surface-variant); }
      .config-tabs { margin-top: 0.5rem; }
      .tab-content { padding: 1rem 0; }
      .json-block {
        background: var(--tch-color-surface-container);
        border-radius: 0.5rem;
        padding: 1rem;
        font-size: 0.8125rem;
        overflow-x: auto;
        white-space: pre-wrap;
        word-break: break-all;
        margin: 0;
        max-height: 500px;
        overflow-y: auto;
      }
    `,
  ],
})
export class AdminConfigPage implements OnInit {
  private readonly api = inject(TenantConfigApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly config = signal<unknown>(null);
  readonly commConfig = signal<unknown>(null);
  readonly docConfig = signal<unknown>(null);

  readonly rawJson = () => JSON.stringify(this.config(), null, 2);
  readonly localeJson = () => {
    const cfg = this.config() as Record<string, unknown> | null;
    return JSON.stringify(cfg?.['locale'] ?? cfg, null, 2);
  };
  readonly commJson = () => JSON.stringify(this.commConfig(), null, 2);
  readonly docJson = () => JSON.stringify(this.docConfig(), null, 2);

  readonly health = signal<ConfigHealth>({
    locale: 'unknown',
    communication: 'unknown',
    document: 'unknown',
    rules: 'unknown',
    games: 'unknown',
  });

  readonly healthItems = computed(() => [
    { key: 'locale', label: 'Locale', state: this.health().locale },
    { key: 'communication', label: 'Communication', state: this.health().communication },
    { key: 'document', label: 'Document', state: this.health().document },
    { key: 'rules', label: 'Règles', state: this.health().rules },
    { key: 'games', label: 'Jeux', state: this.health().games },
  ]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.getTenantConfig().subscribe({
      next: v => {
        this.config.set(v);
        this.loading.set(false);
        this.evaluateHealth(v);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });

    this.api.getCommunicationConfig().subscribe({
      next: v => this.commConfig.set(v),
      error: () => {},
    });

    this.api.getDocumentConfig().subscribe({
      next: v => this.docConfig.set(v),
      error: () => {},
    });
  }

  private evaluateHealth(cfg: unknown): void {
    const c = cfg as Record<string, unknown> | null;
    const hasLocale = !!(c?.['locale'] || c?.['defaultLanguage'] || c?.['defaultLocale']);
    const hasComm = this.commConfig() != null;
    const hasDoc = this.docConfig() != null;

    this.health.set({
      locale: hasLocale ? 'valid' : 'unknown',
      communication: hasComm ? 'valid' : 'unknown',
      document: hasDoc ? 'valid' : 'unknown',
      rules: 'unknown',
      games: 'unknown',
    });
  }
}
