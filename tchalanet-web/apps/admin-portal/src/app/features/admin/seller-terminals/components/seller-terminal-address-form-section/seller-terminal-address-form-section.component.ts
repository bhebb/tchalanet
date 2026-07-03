import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormField } from '@angular/forms/signals';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-seller-terminal-address-form-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminSectionCardComponent,
    TranslatePipe,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './seller-terminal-address-form-section.component.html',
})
export class SellerTerminalAddressFormSectionComponent {
  readonly form = input.required<any>();
}
