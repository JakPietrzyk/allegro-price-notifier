import {PriceHistory} from './price-history.model';

export interface ProductDetails {
  id: number;
  productName: string;
  productUrl: string;
  currentPrice: number;
  priceHistory: PriceHistory[];
}
