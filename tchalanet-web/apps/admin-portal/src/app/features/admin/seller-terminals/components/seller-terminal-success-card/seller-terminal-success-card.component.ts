import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CreateSellerTerminalResult } from '../../../seller-terminal-api.service';

@Component({
  selector: 'tch-seller-terminal-success-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './seller-terminal-success-card.component.html',
  styleUrls: ['./seller-terminal-success-card.component.scss'],
})
export class SellerTerminalSuccessCardComponent {
  readonly result = input.required<CreateSellerTerminalResult>();

  readonly openPos      = output<CreateSellerTerminalResult>();
  readonly backToList   = output<void>();
  readonly createAnother = output<void>();

  readonly showPin = signal(false);
  readonly copied  = signal(false);

  togglePin(): void { this.showPin.update(v => !v); }

  copyCredentials(): void {
    const r = this.result();
    navigator.clipboard.writeText(
      `Code terminal: ${r.terminalCode}\nPIN initial: ${r.initialPin}`,
    ).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2500);
    });
  }
}
