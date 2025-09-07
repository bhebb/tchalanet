import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NxWelcome } from './nx-welcome';

@Component({
  imports: [NxWelcome, RouterModule],
  selector: 'tch-root',
  template: `<tch-nx-welcome></tch-nx-welcome> <router-outlet></router-outlet>`,
  styles: ``,
})
export class App {
  protected title = 'tchalanet-mobile';
}
