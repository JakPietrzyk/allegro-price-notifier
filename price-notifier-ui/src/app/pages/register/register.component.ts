import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import {REGISTER_CONSTANTS} from '../../core/constants/register.constants';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly texts = REGISTER_CONSTANTS;

  firstName = signal<string>('');
  lastName = signal<string>('');
  email = signal<string>('');
  password = signal<string>('');

  errorMessage = signal<string>('');
  loading = signal<boolean>(false);

  onSubmit() {
    if (this.loading()) return;

    if (!this.email() || !this.password()) {
      this.errorMessage.set(this.texts.MESSAGES.ERRORS.INVALID_FORM);
      return;
    }

    this.errorMessage.set('');
    this.loading.set(true);

    const payload = {
      firstName: this.firstName(),
      lastName: this.lastName(),
      email: this.email(),
      password: this.password()
    };

    this.authService.register(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set(this.texts.MESSAGES.ERRORS.GENERIC);
        this.loading.set(false);
      }
    });
  }
}
