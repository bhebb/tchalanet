import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { AdminPricingApi, PricingView } from '../../admin-pricing-api.service';

@Component({
  selector: 'tch-admin-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './admin-pricing.page.html',
  styleUrls: ['./admin-pricing.page.scss'],
})
export class AdminPricingPage implements OnInit {
  private readonly api       = inject(AdminPricingApi);
  private readonly translate = inject(TranslateService);

  readonly columns = ['gameCode', 'betType', 'betOption', 'odds', 'active'];

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly odds = signal<PricingView[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getDefaultOdds({ suppressShellFeedback: true }).subscribe({
      next: v => { this.odds.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err));
        this.loading.set(false);
      },
    });
  }

  activeTone(active: boolean): AdminStatusTone {
    return active ? 'success' : 'neutral';
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.controls.pricing', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      severity: 'error',
      title: this.translate.instant('common.errors.categories.unexpected.title'),
      message: this.translate.instant('common.errors.categories.unexpected.message'),
    };
  }
}
