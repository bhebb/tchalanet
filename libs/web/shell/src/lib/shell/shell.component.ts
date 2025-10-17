import { ChangeDetectionStrategy, Component, HostBinding, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tchl-shell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent {
  // Active l’aside si fourni par le parent
  hasSidebar = input(false);

  // Largeur de la sidebar (overridable en CSS: --tch-side-w)
  sideWidth = input<number>(280);

  // Ajoute une classe hôte pour cibler le layout côté SCSS
  @HostBinding('class.with-side') get withSide() { return this.hasSidebar(); }
}
