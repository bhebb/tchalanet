import { ChangeDetectionStrategy, Component, OnInit, input, output, signal } from '@angular/core';
import { FormField, form, min, required, submit } from '@angular/forms/signals';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';

import {
  ConsolePricingFormValue,
  EMPTY_CONSOLE_PRICING_FORM_VALUE,
} from './console-pricing-form.models';
import { ConsoleBetLabelPipe, ConsoleBetTypeLabelPipe, ConsoleGameNamePipe } from '../games/console-game-labels.pipe';

@Component({
  selector: 'tch-console-pricing-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ConsoleBetLabelPipe,
    ConsoleBetTypeLabelPipe,
    ConsoleGameNamePipe,
    TranslatePipe,
  ],
  templateUrl: './console-pricing-form.component.html',
})
export class ConsolePricingFormComponent implements OnInit {
  readonly initialValue = input<ConsolePricingFormValue>(EMPTY_CONSOLE_PRICING_FORM_VALUE);
  readonly betTypes = input<readonly string[]>([]);
  readonly readonlyIdentity = input(false);
  readonly showActive = input(true);

  readonly submitted = output<ConsolePricingFormValue>();

  readonly model = signal<ConsolePricingFormValue>(EMPTY_CONSOLE_PRICING_FORM_VALUE);
  readonly form = form(this.model, path => {
    required(path.gameCode, { message: 'console.pricingForm.validation.gameCodeRequired' });
    required(path.betType, { message: 'console.pricingForm.validation.betTypeRequired' });
    min(path.odds, 0.01, { message: 'console.pricingForm.validation.oddsMin' });
  });

  ngOnInit(): void {
    this.model.set(this.initialValue());
  }

  invalid(): boolean {
    return this.form().invalid();
  }

  submit(): void {
    submit(this.form, async () => {
      this.submitted.emit(this.model());
    });
  }
}
