import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Brand } from '@tchl/types';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'tch-brand',
  standalone: true,
  imports: [CommonModule, MatIconModule, RouterLink],
  template: `
    <a class="brand" [routerLink]="homeLink()" (click)="onClick($event)" aria-label="Tchalanet">
      @if (brand()?.logo) {
      <img class="brand__logo" [src]="brand()!.logo!" alt="" width="28" height="28" />
      } @if (showName()){
      <span class="brand__name">{{ brand()?.name || 'TCHALANET' }}</span>
      }
    </a>
  `,
  styles: ` 
  .brand {
    display: inline-flex;
    align-items: center;
    gap: .5rem;
    min-inline-size: 0;
  }

  .brand__logo {
    width: 28px;
    height: 28px;
    display: block;
    object-fit: contain;
    flex: 0 0 auto;
  }

  .brand__name {
    line-height: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-inline-size: 48vw;
    min-inline-size: 0;
  }

  a, a:visited {
    color: inherit;
    text-decoration: none;
  }

   a:hover, a:focus-visible {
    text-decoration: underline;
  }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandComponent {
  brand = input<Brand>();
  homeLink = input<string>('/');
  navigateHome = output<void>(); // ← nouvel output
  showName = input<boolean>(true);

  onClick(e: MouseEvent) {
    e.preventDefault();
    this.navigateHome.emit();
  }
}
