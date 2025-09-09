import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ErrorBlockComponent } from '../error-block/error-block.component';

@Component({
  standalone: true,
  imports: [CommonModule, ErrorBlockComponent],
  template: `
    <section class="container" style="padding-block: clamp(24px,6vw,64px);">
      <tchl-error-block [titleKey]="'errors.notFound.title'" [descKey]="'errors.notFound.desc'">
      </tchl-error-block>
    </section>
  `,
})
export class NotFoundComponent {}
