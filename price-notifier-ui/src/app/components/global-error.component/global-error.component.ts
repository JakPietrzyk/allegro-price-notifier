import {Component, inject} from '@angular/core';
import {GlobalErrorService} from '../../services/error/global-error.service';

@Component({
  selector: 'app-global-error',
  imports: [],
  templateUrl: './global-error.component.html',
  styleUrl: './global-error.component.scss',
})
export class GlobalErrorComponent {
  protected errorService = inject(GlobalErrorService);
}
