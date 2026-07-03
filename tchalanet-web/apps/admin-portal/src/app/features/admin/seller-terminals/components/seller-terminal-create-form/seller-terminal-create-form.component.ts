import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormField } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { TchErrorPanel } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  ConsoleAddressFormSectionComponent,
  ConsolePersonIdentityFormSectionComponent,
} from '@tch/web/console';
import { ErrorViewModel } from '@tch/web/errors';

import { SellerTerminalCreateFormModel } from '../../pages/new/admin-seller-terminal-new.page';
import { SellerTerminalPreviewCardComponent } from '../seller-terminal-preview-card/seller-terminal-preview-card.component';

@Component({
  selector: 'tch-seller-terminal-create-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    FormField,
    AdminSectionCardComponent,
    ConsoleAddressFormSectionComponent,
    ConsolePersonIdentityFormSectionComponent,
    SellerTerminalPreviewCardComponent,
    TchErrorPanel,
    TranslatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTooltipModule,
  ],
  templateUrl: './seller-terminal-create-form.component.html',
  styleUrls: ['./seller-terminal-create-form.component.scss'],
})
export class SellerTerminalCreateFormComponent {
  readonly form = input.required<any>();
  readonly model = input.required<SellerTerminalCreateFormModel>();
  readonly saving = input(false);
  readonly error = input<ErrorViewModel | null>(null);
  readonly showPin = input(false);
  readonly showConfirmPin = input(false);
  readonly pinMismatch = input(false);

  readonly create = output<void>();
  readonly regenerateCode = output<void>();
  readonly toggleShowPin = output<void>();
  readonly toggleShowConfirmPin = output<void>();

  pinLength(controlName: 'initialPin' | 'confirmPin'): number {
    return this.model()[controlName].length;
  }
}
