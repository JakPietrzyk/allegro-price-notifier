import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ProductObservation } from '../../models/product-observation.model';
import { ProductService } from '../../services/product/product.service';
import { AuthService } from '../../services/auth/auth.service';
import {DASHBOARD_CONSTANTS} from '../../core/constants/dashboard.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly authService = inject(AuthService);

  protected readonly texts = DASHBOARD_CONSTANTS;

  products = signal<ProductObservation[]>([]);
  loading = signal<boolean>(false);

  newProductName = signal<string>('');
  newProductUrl = signal<string>('');

  ngOnInit() {
    this.loadProducts();
  }

  logout() {
    this.authService.logout();
  }

  loadProducts() {
    this.loading.set(true);
    this.productService.getAll().subscribe({
      next: (data) => {
        this.products.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  addProductByName() {
    const name = this.newProductName();
    if (!name) return;

    this.executeAddOperation(
      this.productService.addByName(name),
      () => this.newProductName.set('')
    );
  }

  addProductByUrl() {
    const url = this.newProductUrl();
    if (!url) return;

    this.executeAddOperation(
      this.productService.addByUrl(url),
      () => this.newProductUrl.set('')
    );
  }

  private executeAddOperation(operation: Observable<unknown>, resetInput: () => void) {
    this.loading.set(true);

    operation.subscribe({
      next: () => {
        resetInput();
        this.loadProducts();
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  deleteProduct(id: number | undefined, event: Event) {
    event.stopPropagation();
    if (!id) return;

    if (!confirm(this.texts.MESSAGES.CONFIRM_DELETE)) {
      return;
    }

    this.loading.set(true);

    this.productService.delete(id).subscribe({
      next: () => {
        this.products.update(current => current.filter(p => p.id !== id));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
}
