import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchConfirmDialog, TchConfirmDialogData, TchStatusBadge } from '@tch/ui/components';
import type { BadgeStatus } from '@tch/ui/components';
import { ConsolePersonIdentitySummaryComponent } from '@tch/web/console';

import { adminUserStatusBadge } from './platform-admin-user.utils';
import type { AdminUserCardData } from './admin-user-card.model';

@Component({
  selector: 'tch-platform-admin-user-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    TchStatusBadge,
    ConsolePersonIdentitySummaryComponent,
    TranslatePipe,
  ],
  templateUrl: './platform-admin-user-card.component.html',
  styleUrls: ['./platform-admin-user-card.component.scss'],
})
export class PlatformAdminUserCardComponent {
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly user = input.required<AdminUserCardData>();

  readonly activate = output<void>();
  readonly block = output<void>();
  readonly archive = output<void>();
  readonly resetPassword = output<void>();
  readonly assignTenant = output<void>();

  statusBadge(status: string): BadgeStatus {
    return adminUserStatusBadge(status);
  }

  canActivate(status: string): boolean {
    return ['SUSPENDED', 'INACTIVE', 'PENDING'].includes(status);
  }

  canBlock(status: string): boolean {
    return ['ACTIVE', 'PENDING'].includes(status);
  }

  canArchive(status: string): boolean {
    return status !== 'ARCHIVED';
  }

  confirmActivate(): void {
    const u = this.user();
    this.openConfirm(
      'common.activate',
      'platform.tenantAdmins.confirm.activate',
      { name: this.userName(u) },
    ).subscribe(r => { if (r?.confirmed) this.activate.emit(); });
  }

  confirmBlock(): void {
    const u = this.user();
    this.openConfirm(
      'common.block',
      'platform.tenantAdmins.confirm.block',
      { name: this.userName(u) },
      true,
    ).subscribe(r => { if (r?.confirmed) this.block.emit(); });
  }

  confirmArchive(): void {
    const u = this.user();
    this.openConfirm(
      'common.archive',
      'platform.tenantAdmins.confirm.archive',
      { name: this.userName(u) },
      true,
    ).subscribe(r => { if (r?.confirmed) this.archive.emit(); });
  }

  confirmResetPassword(): void {
    const u = this.user();
    this.openConfirm(
      'common.resetPassword',
      'platform.tenantAdmins.confirm.resetPassword',
      { name: this.userName(u) },
    ).subscribe(r => { if (r?.confirmed) this.resetPassword.emit(); });
  }

  private openConfirm(titleKey: string, messageKey: string, params: Record<string, string>, destructive = false) {
    return this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant(titleKey, params),
          message: this.translate.instant(messageKey, params),
          confirmLabel: this.translate.instant('common.confirm'),
          destructive,
        },
      })
      .afterClosed();
  }

  private userName(user: AdminUserCardData): string {
    return user.displayName || user.email || user.id;
  }
}
