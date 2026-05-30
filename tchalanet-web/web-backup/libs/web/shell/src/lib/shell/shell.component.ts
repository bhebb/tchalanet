import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, HostBinding, input } from '@angular/core';

@Component({
  selector: 'tchl-shell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    // largeur "logique" de la sidebar (peut être overridée via CSS var)
    '[style.--tch-side-w.px]': 'sideWidth()',
  },
})
export class ShellComponent {
  // est-ce qu'on affiche une colonne side ?
  hasSidebar = input(false);

  // largeur souhaitée de la colonne side
  sideWidth = input<number>(280);

  // classe d'état pour cibler les layouts dans le SCSS
  @HostBinding('class.tch-shell--has-side')
  get sideClass() {
    return this.hasSidebar();
  }
}
