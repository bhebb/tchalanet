import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Outer layout for form pages: constrains width and stacks sections vertically.
 * Use AdminFormSection inside for each card section.
 */
@Component({
  selector: 'tch-admin-form-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="form-shell" [style.max-width]="maxWidth()">
      <ng-content />
    </div>
  `,
  styles: [
    `
      .form-shell {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
        width: 100%;
        margin-inline: auto;
      }
    `,
  ],
})
export class AdminFormShell {
  readonly maxWidth = input('720px');
}
