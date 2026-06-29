import { Directive, DoCheck, Input, inject } from '@angular/core';

import { AdminSectionCardComponent, AdminSectionCardError } from './admin-section-card.component';

export interface AdminSectionTargetError extends AdminSectionCardError {
  readonly target?: string;
}

@Directive({
  selector: 'tch-admin-section-card[tchSectionErrorTarget]',
  standalone: true,
})
export class AdminSectionErrorTargetDirective implements DoCheck {
  private readonly section = inject(AdminSectionCardComponent);
  private currentError: AdminSectionTargetError | null = null;

  @Input({ required: true })
  tchSectionErrorTarget = '';

  @Input()
  tchSectionErrors: readonly AdminSectionTargetError[] = [];

  ngDoCheck(): void {
    const error = this.tchSectionErrors.find(item => item.target === this.tchSectionErrorTarget) ?? null;
    if (error !== this.currentError) {
      this.currentError = error;
      this.section.setSectionError(error);
    }
  }
}
