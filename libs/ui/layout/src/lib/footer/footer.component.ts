import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { FooterProperties } from '@tchl/types';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatDivider } from '@angular/material/divider';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tchl-footer',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    TranslatePipe,
    MatIconModule,
    MatDivider,
  ],
  styleUrl: 'footer.component.scss',
  templateUrl: 'footer.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  properties = input.required<FooterProperties>();

  constructor(iconReg: MatIconRegistry, sanitizer: DomSanitizer) {
    iconReg.addSvgIcon('x', sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/x.svg'));
    iconReg.addSvgIcon(
      'facebook',
      sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/facebook.svg'),
    );
    iconReg.addSvgIcon(
      'youtube',
      sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/youtube.svg'),
    );
    iconReg.addSvgIcon(
      'instagram',
      sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/instagram.svg'),
    );
    iconReg.addSvgIcon(
      'linkedin',
      sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/linkedin.svg'),
    );
  }
}
