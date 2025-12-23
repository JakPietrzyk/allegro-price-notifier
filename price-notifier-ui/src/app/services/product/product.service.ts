import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductObservation } from '../../models/product-observation.model';
import { ProductDetails } from '../../models/product-details.model';
import {PRODUCT_CONSTANTS} from '../../core/constants/product.constants';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<ProductObservation[]> {
    return this.http.get<ProductObservation[]>(PRODUCT_CONSTANTS.API.BASE);
  }

  getById(id: string | number): Observable<ProductDetails> {
    return this.http.get<ProductDetails>(PRODUCT_CONSTANTS.API.BY_ID(id));
  }

  addByName(productName: string): Observable<void> {
    return this.http.post<void>(PRODUCT_CONSTANTS.API.SEARCH, { productName });
  }

  addByUrl(productUrl: string): Observable<void> {
    return this.http.post<void>(PRODUCT_CONSTANTS.API.URL_ADD, { productUrl });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(PRODUCT_CONSTANTS.API.BY_ID(id));
  }
}
