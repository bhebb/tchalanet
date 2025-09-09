import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule }  from '@angular/material/button';
import { MatIconModule }    from '@angular/material/icon';
import { MatMenuModule }    from '@angular/material/menu';
import { HeaderModel } from 'shared/types';
import { UpperCasePipe } from '@angular/common';

@Component({
  selector: 'lib-header',
  standalone: true,
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule, RouterLink, UpperCasePipe],
  template: `
<mat-toolbar color="primary">
  @if (model().logoUrl) {
    <button mat-button routerLink="/"><img [src]="model()!.logoUrl!" alt="" style="height:24px;margin-right:8px"/></button>
  } @else {
    <button mat-button routerLink="/">Tchalanet</button>
  }

  <!-- Menu principal -->
  @for (m of model().menu ?? []; track m.id) {
    @if (m.route) {
      <button mat-button [routerLink]="m.route" [class.mat-mdc-button-base]="m.active">{{ m.label }}</button>
    } @else if (m.href) {
      <a mat-button [href]="m.href" rel="noopener">{{ m.label }}</a>
    }
    @if (m.children?.length) {
      <button mat-button [matMenuTriggerFor]="menu"><mat-icon>arrow_drop_down</mat-icon></button>
      <mat-menu #menu="matMenu">
        @for (c of m.children!; track c.id) {
          @if (c.route) { <button mat-menu-item [routerLink]="c.route">{{ c.label }}</button> }
          @else if (c.href) { <a mat-menu-item [href]="c.href">{{ c.label }}</a> }
        }
      </mat-menu>
    }
  }

  <span class="flex-1"></span>

  <!-- Sélecteur langue -->
  @if ((model().langs?.length ?? 0) > 0) {
    <button mat-button [matMenuTriggerFor]="langMenu">{{ (model().currentLang ?? 'fr') | uppercase }}</button>
    <mat-menu #langMenu="matMenu">
      @for (l of model()!.langs!; track l) {
        <button mat-menu-item (click)="langChange.emit(l)">{{ l | uppercase }}</button>
      }
    </mat-menu>
  }

  <!-- CTA -->
  @if (model().cta) {
    <a mat-raised-button color="accent" [href]="model()!.cta!.href">{{ model()!.cta!.label }}</a>
  }
</mat-toolbar>
  `,
  styles: [`.flex-1{flex:1 1 auto}`],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  model = input.required<HeaderModel>();
  langChange = output<string>();
}
