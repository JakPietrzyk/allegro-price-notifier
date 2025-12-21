import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartOptions } from 'chart.js';

// Interfejsy dopasowane do backendu
interface PriceHistory {
  price: number;
  checkedAt: string;
}

interface ProductDetails {
  id: number;
  productName: string;
  productUrl: string;
  currentPrice: number;
  priceHistory: PriceHistory[];
}

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective], // BaseChartDirective jest kluczowe
  providers: [DatePipe, DecimalPipe],
  templateUrl: './product-details.component.html',
  styleUrl: './product-details.component.css'
})
export class ProductDetailsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private datePipe = inject(DatePipe);

  product = signal<ProductDetails | null>(null);
  loading = signal<boolean>(true);
  lowestPrice = signal<number>(0);

  // Konfiguracja Wykresu
  public lineChartData: ChartConfiguration<'line'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Cena (PLN)',
        fill: true,
        tension: 0.4, // Wygładzenie linii
        borderColor: '#3f51b5',
        backgroundColor: 'rgba(63, 81, 181, 0.2)'
      }
    ]
  };

  public lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
      tooltip: { mode: 'index', intersect: false }
    },
    scales: {
      y: { beginAtZero: false } // Skaluj oś Y dynamicznie, nie od zera
    }
  };

  public lineChartLegend = true;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadDetails(id);
    }
  }

  loadDetails(id: string) {
    this.http.get<ProductDetails>(`${environment.apiUrl}/products/${id}`)
      .subscribe({
        next: (data) => {
          this.product.set(data);
          this.processChartData(data.priceHistory);
          this.calculateStats(data);
          this.loading.set(false);
        },
        error: (err) => {
          console.error(err);
          this.loading.set(false);
        }
      });
  }

  private calculateStats(data: ProductDetails) {
    if (!data.priceHistory || data.priceHistory.length === 0) {
      this.lowestPrice.set(data.currentPrice);
      return;
    }
    // Znajdź najniższą cenę w historii
    const min = Math.min(...data.priceHistory.map(h => h.price));
    this.lowestPrice.set(min);
  }

  private processChartData(history: PriceHistory[]) {
    // Sortujemy po dacie (rosnąco)
    const sorted = [...history].sort((a, b) =>
      new Date(a.checkedAt).getTime() - new Date(b.checkedAt).getTime()
    );

    // Etykiety osi X (Daty)
    const labels = sorted.map(h =>
      this.datePipe.transform(h.checkedAt, 'dd.MM HH:mm') || ''
    );

    // Dane osi Y (Ceny)
    const prices = sorted.map(h => h.price);

    this.lineChartData = {
      ...this.lineChartData,
      labels: labels,
      datasets: [{
        ...this.lineChartData.datasets[0],
        data: prices
      }]
    };
  }
}
