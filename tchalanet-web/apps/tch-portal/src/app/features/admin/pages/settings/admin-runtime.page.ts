import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { RuntimeApiService, TenantRuntimeView } from '../../runtime-api.service';

const LANGUAGE_LABELS: Record<string, string> = {
  fr: 'Français',
  en: 'English',
  ht: 'Kreyòl Ayisyen',
};

function languageLabel(code: string): string {
  return LANGUAGE_LABELS[code] ?? code;
}

@Component({
  selector: 'tch-admin-runtime-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Runtime tenant"
      description="Configuration d'exécution du tenant courant."
    >
      <div actions>
        <button mat-stroked-button [disabled]="loading()" (click)="reload()">
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
      } @else if (!runtime()) {
        <tch-admin-empty-state
          icon="dns"
          title="Aucune donnée runtime"
          message="Impossible de récupérer les informations runtime."
        />
      } @else {
        <div class="sections">
          <!-- Runtime Info -->
          <section class="info-section">
            <h3 class="section-title">Informations runtime</h3>
            <dl class="info-list">
              <dt>Code</dt>
              <dd>{{ runtime()!.code }}</dd>
              <dt>Nom</dt>
              <dd>{{ runtime()!.name }}</dd>
              <dt>Statut</dt>
              <dd>
                <span class="status-badge" [attr.data-status]="runtime()!.status">
                  {{ runtime()!.status }}
                </span>
              </dd>
              <dt>Langue par défaut</dt>
              <dd>{{ runtime()!.defaultLanguage ? languageLabel(runtime()!.defaultLanguage!) : '—' }}</dd>
              <dt>Locale par défaut</dt>
              <dd>{{ runtime()!.defaultLocale ?? '—' }}</dd>
            </dl>
          </section>

          <!-- Regional Config -->
          <section class="info-section">
            <h3 class="section-title">Configuration régionale</h3>
            <dl class="info-list">
              <dt>Fuseau horaire</dt>
              <dd>{{ runtime()!.timezone }}</dd>
              <dt>Devise</dt>
              <dd>{{ runtime()!.currency }}</dd>
            </dl>
          </section>

          <!-- Supported Locales -->
          <section class="info-section">
            <h3 class="section-title">Locales supportées</h3>
            @if (runtime()!.supportedLocales.length === 0) {
              <p class="empty-message">Aucune locale configurée.</p>
            } @else {
              <ul class="locale-list">
                @for (locale of runtime()!.supportedLocales; track locale) {
                  <li class="locale-chip">{{ languageLabel(locale) }} ({{ locale }})</li>
                }
              </ul>
            }
          </section>

          <!-- Raw JSON -->
          <section class="info-section">
            <h3 class="section-title">Données brutes (JSON)</h3>
            <pre class="json-block">{{ runtimeJson() }}</pre>
          </section>

          <!-- Quick Actions -->
          <section class="info-section">
            <h3 class="section-title">Actions rapides</h3>
            <div class="quick-actions">
              <a mat-stroked-button routerLink="/app/admin/business-days">
                <span class="material-symbols-outlined">calendar_today</span>
                Jours ouvrables
              </a>
              <a mat-stroked-button routerLink="/app/admin/games">
                <span class="material-symbols-outlined">casino</span>
                Jeux
              </a>
              <a mat-stroked-button routerLink="/app/admin/settings/config">
                <span class="material-symbols-outlined">tune</span>
                Configuration
              </a>
            </div>
          </section>
        </div>
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
      .trace-id { font-size: 0.75rem; opacity: 0.7; }
      .sections { display: flex; flex-direction: column; gap: 1.25rem; }
      .info-section {
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 0.75rem;
        padding: 1.25rem;
      }
      .section-title { margin: 0 0 0.75rem; font-size: 1rem; font-weight: 600; }
      .info-list {
        display: grid;
        grid-template-columns: auto 1fr;
        gap: 0.25rem 1.5rem;
        margin: 0;
      }
      dt { font-size: 0.8125rem; color: var(--tch-color-on-surface-variant); }
      dd { margin: 0; font-size: 0.875rem; font-weight: 500; }
      .status-badge {
        display: inline-block;
        padding: 0.125rem 0.625rem;
        border-radius: 9999px;
        font-size: 0.75rem;
        font-weight: 600;
        text-transform: uppercase;
      }
      .status-badge[data-status='ACTIVE'] { background: #d4edda; color: #155724; }
      .status-badge[data-status='SUSPENDED'] { background: #fff3cd; color: #856404; }
      .locale-list { display: flex; flex-wrap: wrap; gap: 0.5rem; padding: 0; list-style: none; margin: 0; }
      .locale-chip {
        display: inline-block;
        padding: 0.25rem 0.75rem;
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: 9999px;
        font-size: 0.8125rem;
      }
      .json-block {
        background: var(--tch-color-surface-container);
        border-radius: 0.5rem;
        padding: 1rem;
        font-size: 0.8125rem;
        overflow-x: auto;
        white-space: pre-wrap;
        word-break: break-all;
        margin: 0;
      }
      .quick-actions { display: flex; flex-wrap: wrap; gap: 0.75rem; }
      .empty-message { color: var(--tch-color-on-surface-variant); font-size: 0.875rem; }
    `,
  ],
})
export class AdminRuntimePage implements OnInit {
  private readonly api = inject(RuntimeApiService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly languageLabel = languageLabel;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly runtime = signal<TenantRuntimeView | null>(null);
  readonly runtimeJson = () =>
    this.runtime() ? JSON.stringify(this.runtime(), null, 2) : '';

  ngOnInit(): void {
    this.load();
  }

  reload(): void {
    this.load();
    this.snackBar.open('Données rechargées.', 'OK', { duration: 2000 });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getTenantRuntime().subscribe({
      next: v => { this.runtime.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });
  }
}
