import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormField } from '@angular/forms/signals';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-seller-terminal-identity-form-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminSectionCardComponent,
    TranslatePipe,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTooltipModule,
  ],
  templateUrl: './seller-terminal-identity-form-section.component.html',
})
export class SellerTerminalIdentityFormSectionComponent {
  readonly form = input.required<any>();
  readonly regenerateCode = output<void>();
}
