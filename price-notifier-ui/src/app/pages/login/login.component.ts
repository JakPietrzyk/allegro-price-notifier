import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';
import {LOGIN_CONSTANTS} from '../../core/constants/login.constants';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly texts = LOGIN_CONSTANTS;

  email = signal<string>('');
  password = signal<string>('');
  errorMessage = signal<string>('');
  loading = signal<boolean>(false);

  ngOnInit() {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit() {
    if (this.loading()) return;
    this.errorMessage.set('');
    this.loading.set(true);

    const credentials = {
      email: this.email(),
      password: this.password()
    };

    this.authService.login(credentials).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set(this.texts.MESSAGES.ERRORS.INVALID_CREDENTIALS);
        this.loading.set(false);
      }
    });
  }
}
