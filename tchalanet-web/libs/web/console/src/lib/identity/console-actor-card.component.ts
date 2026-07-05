import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchConfirmDialog, TchConfirmDialogData, TchStatusBadge } from '@tch/ui/components';

import {
  ConsoleActorIdentity,
  consoleActorPrimaryLabel,
  consoleActorStatusBadge,
  consoleActorTenantLabel,
} from './console-actor-identity';
import { ConsolePersonIdentitySummaryComponent } from './console-person-identity-summary.component';

@Component({
  selector: 'tch-console-actor-card',
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
  templateUrl: './console-actor-card.component.html',
  styleUrls: ['./console-actor-card.component.scss'],
})
export class ConsoleActorCardComponent {
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly actor = input.required<ConsoleActorIdentity>();
  readonly showAssignTenant = input(true);
  readonly showManageTenantAdmins = input(true);

  readonly activate = output<void>();
  readonly block = output<void>();
  readonly archive = output<void>();
  readonly resetPassword = output<void>();
  readonly assignTenant = output<void>();

  primaryLabel(actor: ConsoleActorIdentity): string {
    return consoleActorPrimaryLabel(actor);
  }

  tenantLabel(actor: ConsoleActorIdentity): string | null {
    return consoleActorTenantLabel(actor);
  }

  statusBadge(status: string) {
    return consoleActorStatusBadge(status);
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
    const actor = this.actor();
    this.openConfirm(
      'common.activate',
      'platform.tenantAdmins.confirm.activate',
      { name: this.primaryLabel(actor) },
    ).subscribe(result => { if (result?.confirmed) this.activate.emit(); });
  }

  confirmBlock(): void {
    const actor = this.actor();
    this.openConfirm(
      'common.block',
      'platform.tenantAdmins.confirm.block',
      { name: this.primaryLabel(actor) },
      true,
    ).subscribe(result => { if (result?.confirmed) this.block.emit(); });
  }

  confirmArchive(): void {
    const actor = this.actor();
    this.openConfirm(
      'common.archive',
      'platform.tenantAdmins.confirm.archive',
      { name: this.primaryLabel(actor) },
      true,
    ).subscribe(result => { if (result?.confirmed) this.archive.emit(); });
  }

  confirmResetPassword(): void {
    const actor = this.actor();
    this.openConfirm(
      'common.resetPassword',
      'platform.tenantAdmins.confirm.resetPassword',
      { name: this.primaryLabel(actor) },
    ).subscribe(result => { if (result?.confirmed) this.resetPassword.emit(); });
  }

  private openConfirm(
    titleKey: string,
    messageKey: string,
    params: Record<string, string>,
    destructive = false,
  ) {
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
}
