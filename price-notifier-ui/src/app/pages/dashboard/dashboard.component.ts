import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import {environment} from '../../../environments/environment';
import {RouterLink} from '@angular/router';

interface ProductObservation {
  id?: number;
  productName: string;
  currentPrice: number;
  productUrl: string;
  lastChecked?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  products = signal<ProductObservation[]>([]);
  loading = signal<boolean>(false);

  newProductName = signal<string>('');
  newProductUrl = signal<string>('');

  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = `${environment.apiUrl}/products`;
  ngOnInit() {
    this.loadProducts();
  }

  logout() {
    this.authService.logout();
  }

  loadProducts() {
    this.loading.set(true);

    this.http.get<ProductObservation[]>(this.apiUrl)
      .subscribe({
        next: (data) => {
          this.products.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          console.error(err);
          this.loading.set(false);
        }
      });
  }

  addProductByName() {
    const nameValue = this.newProductName();

    if (!nameValue) return;

    this.executeAdd(`${this.apiUrl}/search`, {
      productName: nameValue,
    });

    this.newProductName.set('');
  }

  addProductByUrl() {
    const urlValue = this.newProductUrl();

    if (!urlValue) return;

    this.executeAdd(`${this.apiUrl}/url`, {
      productUrl: urlValue,
    });

    this.newProductUrl.set('');
  }

  private executeAdd(url: string, body: any) {
    this.loading.set(true);

    this.http.post(url, body).subscribe({
      next: () => {
        this.loadProducts();
      },
      error: (err) => {
        alert('Błąd podczas dodawania produktu.');
        this.loading.set(false);
      }
    });
  }

  deleteProduct(id: number | undefined, event: Event) {
    event.stopPropagation();

    if (!id) return;

    const confirmed = window.confirm('Czy na pewno chcesz usunąć ten produkt i jego historię?');

    if (confirmed) {
      this.loading.set(true);

      this.http.delete(`${this.apiUrl}/${id}`).subscribe({
        next: () => {
          this.products.update(currentProducts => currentProducts.filter(p => p.id !== id));
          this.loading.set(false);
        },
        error: (err) => {
          console.error(err);
          alert('Nie udało się usunąć produktu.');
          this.loading.set(false);
        }
      });
    }
  }
}
