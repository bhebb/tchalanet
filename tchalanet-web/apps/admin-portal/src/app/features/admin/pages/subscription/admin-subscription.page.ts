import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '@tch/ui/console';
import {
  AdminSubscriptionApi,
  SubscriptionView,
  SubscriptionStatus,
} from '../../admin-subscription-api.service';
import { RenewSubscriptionDialog } from './dialogs/renew-subscription.dialog';
import { CancelSubscriptionDialog } from './dialogs/cancel-subscription.dialog';

@Component({
  selector: 'tch-admin-subscription-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminSectionErrorTargetDirective,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-subscription.page.html',
  styleUrls: ['./admin-subscription.page.scss'],
})
export class AdminSubscriptionPage implements OnInit {
  private readonly api = inject(AdminSubscriptionApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);
  readonly subscription = signal<SubscriptionView | null>(null);
  readonly acting = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.sectionErrors.set([]);
    this.api.get({ suppressShellFeedback: true }).subscribe({
      next: v => { this.subscription.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'admin.subscription.load'));
        this.loading.set(false);
      },
    });
  }

  openRenew(): void {
    const ref = this.dialog.open(RenewSubscriptionDialog, { width: '420px' });
    ref.afterClosed().subscribe((newEndsAt: string | undefined) => {
      if (!newEndsAt) return;
      this.acting.set(true);
      this.clearSectionError('admin.subscription.actions');
      this.api.renew(newEndsAt, { suppressShellFeedback: true }).subscribe({
        next: () => { this.load(); this.acting.set(false); },
        error: err => { this.setSectionError('admin.subscription.actions', err); this.acting.set(false); },
      });
    });
  }

  openCancel(): void {
    const ref = this.dialog.open(CancelSubscriptionDialog, { width: '420px' });
    ref.afterClosed().subscribe((reason: string | undefined) => {
      if (reason === undefined) return;
      this.acting.set(true);
      this.clearSectionError('admin.subscription.actions');
      this.api.cancel(reason || undefined, { suppressShellFeedback: true }).subscribe({
        next: () => { this.load(); this.acting.set(false); },
        error: err => { this.setSectionError('admin.subscription.actions', err); this.acting.set(false); },
      });
    });
  }

  suspend(): void {
    this.acting.set(true);
    this.clearSectionError('admin.subscription.actions');
    this.api.suspend({ suppressShellFeedback: true }).subscribe({
      next: () => { this.load(); this.acting.set(false); },
      error: err => { this.setSectionError('admin.subscription.actions', err); this.acting.set(false); },
    });
  }

  resume(): void {
    this.acting.set(true);
    this.clearSectionError('admin.subscription.actions');
    this.api.resume({ suppressShellFeedback: true }).subscribe({
      next: () => { this.load(); this.acting.set(false); },
      error: err => { this.setSectionError('admin.subscription.actions', err); this.acting.set(false); },
    });
  }

  statusTone(status: SubscriptionStatus): AdminStatusTone {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'TRIALING': return 'success';
      case 'PAST_DUE': return 'warning';
      case 'SUSPENDED': return 'warning';
      case 'CANCELLED': return 'danger';
      case 'EXPIRED': return 'danger';
    }
  }

  private setSectionError(target: string, err: unknown): void {
    const vm = this.errorViewModel(err, target);
    this.sectionErrors.update(errors => [
      ...errors.filter(error => error.target !== target),
      { ...vm, target },
    ]);
  }

  private clearSectionError(target: string): void {
    this.sectionErrors.update(errors => errors.filter(error => error.target !== target));
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
