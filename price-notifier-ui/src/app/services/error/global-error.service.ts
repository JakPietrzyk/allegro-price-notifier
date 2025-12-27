import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class GlobalErrorService {
  private readonly _error = signal<string | null>(null);

  error = this._error.asReadonly();

  showError(message: string) {
    this._error.set(message);

    setTimeout(() => {
      this.clear();
    }, 5000);
  }

  clear() {
    this._error.set(null);
  }
}
