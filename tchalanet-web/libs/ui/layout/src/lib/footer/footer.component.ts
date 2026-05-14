import { TranslatePipe } from '@ngx-translate/core';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

import { FooterProperties } from '@tchl/types';

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
