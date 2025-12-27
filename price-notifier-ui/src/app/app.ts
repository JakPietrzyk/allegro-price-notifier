import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {GlobalErrorComponent} from './components/global-error.component/global-error.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, GlobalErrorComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('price-notifier-ui');
}
