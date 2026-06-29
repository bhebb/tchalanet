import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeSandboxComponent } from '@tch/web/sandbox';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, ThemeSandboxComponent],
  selector: 'tch-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
