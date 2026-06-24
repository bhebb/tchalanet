import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminPageShellComponent } from '../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../shared/admin-ui/admin-section-card.component';

@Component({
  selector: 'tch-seller-terminal-activation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminSectionCardComponent],
  template: `
    <tch-admin-page-shell
      [title]="'sellerTerminal.activation.title' | translate"
      [description]="'sellerTerminal.activation.description' | translate"
    >
      <tch-admin-section-card [title]="'sellerTerminal.activation.pin.title' | translate" icon="pin">
        <p>{{ 'sellerTerminal.activation.pin.pendingBackend' | translate }}</p>
      </tch-admin-section-card>
    </tch-admin-page-shell>
  `,
})
export class SellerTerminalActivationPage {}
