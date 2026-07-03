import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormField } from '@angular/forms/signals';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-console-person-identity-form-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminSectionCardComponent,
    TranslatePipe,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './console-person-identity-form-section.component.html',
  styleUrls: ['./console-form-sections.scss'],
})
export class ConsolePersonIdentityFormSectionComponent {
  readonly form = input.required<any>();
  readonly titleKey = input('component.console.personIdentity.title');
  readonly descriptionKey = input<string | null>('component.console.personIdentity.description');
  readonly icon = input('badge');
}
