import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'tch-admin-refresh-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <button
      type="button"
      mat-stroked-button
      data-testid="admin-refresh-button"
      [disabled]="disabled()"
      [attr.aria-busy]="busy()"
      (click)="refreshRequested.emit()"
    >
      <mat-icon aria-hidden="true">refresh</mat-icon>
      {{ label() }}
    </button>
  `,
})
export class AdminRefreshButtonComponent {
  readonly label = input.required<string>();
  readonly disabled = input(false);
  readonly busy = input(false);
  readonly refreshRequested = output<void>();
}
