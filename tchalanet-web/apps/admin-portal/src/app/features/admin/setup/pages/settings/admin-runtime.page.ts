import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import { RuntimeApiService, TenantRuntimeView } from '../../data-access/runtime-api.service';

const LANGUAGE_LABELS: Record<string, string> = {
  fr: 'Français',
  en: 'English',
  ht: 'Kreyòl Ayisyen',
};

@Component({
  selector: 'tch-admin-runtime-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-runtime.page.html',
  styleUrls: ['./admin-runtime.page.scss'],
})
export class AdminRuntimePage implements OnInit {
  private readonly api = inject(RuntimeApiService);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly notice = signal<string | null>(null);
  readonly runtime = signal<TenantRuntimeView | null>(null);
  readonly runtimeJson = () =>
    this.runtime() ? JSON.stringify(this.runtime(), null, 2) : '';

  languageLabel(code: string): string {
    return LANGUAGE_LABELS[code] ?? code;
  }

  runtimeStatusTone(status: string): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success',
      DRAFT: 'neutral',
      SUSPENDED: 'warning',
      ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  ngOnInit(): void {
    this.load();
  }

  reload(): void {
    this.load(true);
  }

  private load(showReloadNotice = false): void {
    this.loading.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.api.getTenantRuntime({ suppressShellFeedback: true }).subscribe({
      next: v => {
        this.runtime.set(v);
        this.loading.set(false);
        if (showReloadNotice) this.notice.set('Données rechargées.');
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err));
        this.loading.set(false);
      },
    });
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.runtime', 'page');
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
