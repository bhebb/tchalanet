import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TchConfirmDialog, TchConfirmDialogData, TchStatusBadge } from '@tch/ui/components';
import type { BadgeStatus } from '@tch/ui/components';

import { adminUserStatusBadge } from '../platform-admin-user.utils';
import type { AdminUserCardData } from './admin-user-card.model';

@Component({
  selector: 'tch-platform-admin-user-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, MatButtonModule, TchStatusBadge],
  templateUrl: './platform-admin-user-card.component.html',
  styleUrls: ['./platform-admin-user-card.component.scss'],
})
export class PlatformAdminUserCardComponent {
  private readonly dialog = inject(MatDialog);

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
      'Activer ce compte',
      `Le compte de ${u.displayName || u.email} sera réactivé.`,
    ).subscribe(r => { if (r?.confirmed) this.activate.emit(); });
  }

  confirmBlock(): void {
    const u = this.user();
    this.openConfirm(
      'Bloquer ce compte',
      `Le compte de ${u.displayName || u.email} sera suspendu.`,
      true,
    ).subscribe(r => { if (r?.confirmed) this.block.emit(); });
  }

  confirmArchive(): void {
    const u = this.user();
    this.openConfirm(
      'Archiver ce compte',
      `Le compte de ${u.displayName || u.email} sera archivé. Cette action est irréversible.`,
      true,
    ).subscribe(r => { if (r?.confirmed) this.archive.emit(); });
  }

  confirmResetPassword(): void {
    const u = this.user();
    this.openConfirm(
      'Réinitialiser le mot de passe',
      `Confirmer la réinitialisation du mot de passe de ${u.displayName || u.email}.`,
    ).subscribe(r => { if (r?.confirmed) this.resetPassword.emit(); });
  }

  private openConfirm(title: string, message: string, destructive = false) {
    return this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: { title, message, confirmLabel: 'Confirmer', destructive },
      })
      .afterClosed();
  }
}
